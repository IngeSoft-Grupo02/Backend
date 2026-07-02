package pe.edu.pucp.kingstore.service.cart;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.cart.CartItem;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.repository.cart.ShoppingCartRepository;
import pe.edu.pucp.kingstore.repository.quotation.QuotationRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.domain.dto.cart.CartResponseDTO;
import pe.edu.pucp.kingstore.domain.model.product.Discount;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.repository.product.DiscountRepository;
import pe.edu.pucp.kingstore.domain.dto.product.CustomDesignRequestDTO;
import pe.edu.pucp.kingstore.domain.model.product.CustomDesign;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ShoppingCartService extends AbstractCrudService<ShoppingCart> {

    public static final double DESIGN_FEE_PERCENTAGE = 10.0;

    private final ShoppingCartRepository shoppingCartRepository;
    private final DiscountRepository discountRepository;
    private final QuotationRepository quotationRepository;

    public ShoppingCartService(ShoppingCartRepository shoppingCartRepository,
                               DiscountRepository discountRepository,
                               QuotationRepository quotationRepository) {
        super(shoppingCartRepository, "Shopping cart");
        this.shoppingCartRepository = shoppingCartRepository;
        this.discountRepository = discountRepository;
        this.quotationRepository = quotationRepository;
    }

    @Transactional(readOnly = true)
    public Optional<ShoppingCart> findByCustomer(Integer customerId) {
        requireId(customerId);
        return shoppingCartRepository.findByCustomerIdAndActiveTrueOrderByIdDesc(customerId)
                .stream()
                .findFirst();
    }
    // ── Operaciones del cliente ───────────────────────────────────────────────────

    /**
     * Obtiene un carrito activo del cliente APTO para seguir comprando/cotizando,
     * o crea uno vacío si no hay ninguno.
     *
     * Garantiza el invariante "un carrito ya cotizado no permanece activo":
     * recorre los carritos activos (del más reciente al más antiguo) y
     *   - devuelve el primero que NO tenga cotización asociada;
     *   - desactiva los que ya tengan cotización (estado inconsistente que arrastra
     *     la BD histórica) para que no vuelvan a aparecer como activos.
     * Si no queda ninguno reutilizable, crea un carrito nuevo y vacío.
     *
     * Así nunca se devuelve un carrito "gastado": ni addItem agrega productos sobre
     * un carrito ya cotizado, ni createFromCart intenta recotizarlo, evitando que el
     * cliente quede atrapado y que el carrito se vacíe sin generar cotización.
     */
    @Transactional
    public ShoppingCart getOrCreateCart(Customer customer) {
        for (ShoppingCart cart : shoppingCartRepository
                .findByCustomerIdAndActiveTrueOrderByIdDesc(customer.getId())) {
            if (quotationRepository.findByShoppingCartId(cart.getId()).isEmpty()) {
                return cart;
            }
            cart.setActive(false);
            shoppingCartRepository.save(cart);
        }
        ShoppingCart cart = new ShoppingCart();
        cart.setCustomer(customer);
        cart.setItems(new ArrayList<>());
        cart.setSubTotal(0);
        cart.setDiscount(0);
        cart.setTotalAmount(0);
        return shoppingCartRepository.save(cart);
    }

    /**
     * Agrega una variante al carrito. Si ya existe un item con esa variante,
     * suma la cantidad en lugar de crear un duplicado.
     * Calcula el descuento por volumen aplicable automáticamente.
     */
    @Transactional
    public ShoppingCart addItem(ShoppingCart cart, ProductVariant variant,
                                int quantity, Integer storeId) {
        return addItem(cart, variant, quantity, storeId, false);
    }

    @Transactional
    public ShoppingCart addItem(ShoppingCart cart, ProductVariant variant,
                                int quantity, Integer storeId, boolean separateItem) {
        if (quantity <= 0) {
            throw new BusinessRuleException("Quantity must be positive");
        }
        // El stock NO bloquea la cotización: el cliente puede solicitar cantidades
        // por encima del stock disponible y el comerciante decide si las acepta o rechaza.

        // Solo se acumulan items sin diseño. Si el cliente envía comentario o
        // archivos de diseño, esa línea debe mantenerse independiente.
        CartItem existing = separateItem ? null : cart.getItems().stream()
                .filter(i -> Objects.equals(i.getProductVariant().getId(), variant.getId()))
                .filter(i -> i.getCustomDesign() == null)
                .findFirst()
                .orElse(null);

        if (existing != null) {
            int newQty = existing.getQuantity() + quantity;
            existing.setQuantity(newQty);
            priceCartItem(existing, storeId);
        } else {
            CartItem item = new CartItem();
            item.setProductVariant(variant);
            item.setQuantity(quantity);
            priceCartItem(item, storeId);
            cart.getItems().add(item);
        }

        recalculateTotals(cart);
        return shoppingCartRepository.save(cart);
    }

    /**
     * Actualiza la cantidad de un item existente en el carrito.
     * Recalcula precio y descuento por volumen con la nueva cantidad.
     */
    @Transactional
    public ShoppingCart updateItem(ShoppingCart cart, Integer cartItemId,
                                   int quantity, Integer storeId) {
        if (quantity <= 0) {
            throw new BusinessRuleException("Quantity must be positive");
        }
        CartItem item = cart.getItems().stream()
                .filter(i -> Objects.equals(i.getId(), cartItemId))
                .findFirst()
                .orElseThrow(() -> new pe.edu.pucp.kingstore.service.common
                        .ResourceNotFoundException("Cart item", cartItemId));

        ProductVariant variant = item.getProductVariant();
        // El stock NO bloquea la actualización del carrito (mismo criterio que addItem):
        // la cotización admite cantidades por encima del stock disponible.

        item.setQuantity(quantity);
        priceCartItem(item, storeId);

        recalculateTotals(cart);
        return shoppingCartRepository.save(cart);
    }

    /**
     * Elimina un item del carrito por su id.
     */
    @Transactional
    public ShoppingCart removeItem(ShoppingCart cart, Integer cartItemId) {
        boolean removed = cart.getItems()
                .removeIf(i -> Objects.equals(i.getId(), cartItemId));
        if (!removed) {
            throw new pe.edu.pucp.kingstore.service.common
                    .ResourceNotFoundException("Cart item", cartItemId);
        }
        recalculateTotals(cart);
        return shoppingCartRepository.save(cart);
    }
    /**
     * Registra o reemplaza la personalización de un item del carrito.
     * Si el item ya tenía un diseño, lo sobreescribe.
     */
    @Transactional
    public ShoppingCart addDesignToItem(ShoppingCart cart, Integer cartItemId,
                                        CustomDesignRequestDTO request) {
        if (request == null) {
            throw new BusinessRuleException("Design request is required");
        }
        if ((request.getImageUrl() == null || request.getImageUrl().isBlank())
                && (request.getDescription() == null || request.getDescription().isBlank())) {
            throw new BusinessRuleException("Design must have an image or a description");
        }

        CartItem item = cart.getItems().stream()
                .filter(i -> Objects.equals(i.getId(), cartItemId))
                .findFirst()
                .orElseThrow(() -> new pe.edu.pucp.kingstore.service.common
                        .ResourceNotFoundException("Cart item", cartItemId));

        CustomDesign design = item.getCustomDesign() != null
                ? item.getCustomDesign()
                : new CustomDesign();

        design.setImageUrl(request.getImageUrl());
        design.setDescription(request.getDescription());
        design.setOverlayX(normalizePercent(request.getOverlayX(), 50));
        design.setOverlayY(normalizePercent(request.getOverlayY(), 42));
        design.setOverlayWidth(normalizePercent(request.getOverlayWidth(), 24));
        design.setOverlayHeight(normalizePercent(request.getOverlayHeight(), 18));
        design.setProduct(item.getProductVariant().getProduct());

        item.setCustomDesign(design);

        Integer storeId = item.getProductVariant().getProduct().getStore().getId();
        priceCartItem(item, storeId);
        recalculateTotals(cart);
        return shoppingCartRepository.save(cart);
    }
// ── Entity → DTO ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public CartResponseDTO toResponseDTO(ShoppingCart cart) {
        ShoppingCart responseCart = cart;
        if (cart.getId() != null) {
            Optional<ShoppingCart> loadedCart = shoppingCartRepository.findWithItemsById(cart.getId());
            if (loadedCart != null && loadedCart.isPresent()) {
                responseCart = loadedCart.get();
            }
        }

        List<CartResponseDTO.CartItemResponseDTO> items = responseCart.getItems() == null
                ? List.of()
                : responseCart.getItems().stream().map(item -> {
            ProductVariant variant = item.getProductVariant();
            Product product = variant.getProduct();
            double originalPrice = product.getBasePrice();
            DiscountMatch discount = resolveDiscount(
                    product.getStore().getId(), product, item.getQuantity());
            PriceBreakdown pricing = priceBreakdown(
                    originalPrice, item.getQuantity(), discount.percentage(), hasCustomDesign(item));
            CartResponseDTO.CustomDesignResponseDTO designDTO = null;
            if (item.getCustomDesign() != null) {
                CustomDesign d = item.getCustomDesign();
                designDTO = new CartResponseDTO.CustomDesignResponseDTO(
                        d.getId(),
                        d.getImageUrl(),
                        d.getDescription(),
                        d.getObservations(),
                        d.getSentAt(),
                        d.getOverlayX(),
                        d.getOverlayY(),
                        d.getOverlayWidth(),
                        d.getOverlayHeight()
                );
            }
            return new CartResponseDTO.CartItemResponseDTO(
                    item.getId(),
                    product.getId(),
                    variant.getId(),
                    product.getName(),
                    variant.getSize(),
                    variant.getColor(),
                    round(pricing.lineTotal() / item.getQuantity()),
                    item.getQuantity(),
                    pricing.lineTotal(),
                    round(discount.percentage()),
                    designDTO,
                    firstProductImage(product),
                    round(originalPrice),
                    pricing.baseSubtotal(),
                    pricing.discountAmount(),
                    pricing.designFeeAmount(),
                    pricing.lineTotal(),
                    discount.ruleLabel(),
                    pricing.designFeeAmount() > 0
            );
        }).toList();

        double productSubtotal = items.stream()
                .mapToDouble(CartResponseDTO.CartItemResponseDTO::getBaseSubtotal)
                .sum();
        double discountTotal = items.stream()
                .mapToDouble(CartResponseDTO.CartItemResponseDTO::getDiscountAmount)
                .sum();
        double designFeeTotal = items.stream()
                .mapToDouble(CartResponseDTO.CartItemResponseDTO::getDesignFeeAmount)
                .sum();

        return new CartResponseDTO(
                responseCart.getId(),
                items,
                responseCart.getSubTotal(),
                responseCart.getDiscount(),
                responseCart.getTotalAmount(),
                round(productSubtotal),
                round(discountTotal),
                round(designFeeTotal)
        );
    }

// ── Helpers privados ──────────────────────────────────────────────────────────

    private String firstProductImage(Product product) {
        if (product == null || product.getImageUrls() == null) {
            return null;
        }
        return product.getImageUrls().stream()
                .filter(url -> url != null && !url.isBlank())
                .findFirst()
                .orElse(null);
    }

    /**
     * Busca el descuento por volumen activo más beneficioso para el producto
     * según la cantidad solicitada. Retorna el porcentaje a aplicar (0 si ninguno aplica).
     */
    private DiscountMatch resolveDiscount(Integer storeId, Product product, int quantity) {
        if (storeId == null || product == null) {
            return new DiscountMatch(0, null);
        }
        List<Discount> discounts = Optional.ofNullable(discountRepository.findByStoreId(storeId))
                .orElse(List.of());
        return discounts.stream()
                .filter(d -> Boolean.TRUE.equals(d.getActive()))
                .filter(d -> !Boolean.TRUE.equals(d.getDeleted()))
                .filter(d -> d.getProduct() == null
                        || Objects.equals(d.getProduct().getId(), product.getId()))
                .filter(d -> appliesToQuantity(d, quantity))
                .max((left, right) -> Double.compare(
                        left.getDiscountPercentage(), right.getDiscountPercentage()))
                .map(d -> new DiscountMatch(
                        d.getDiscountPercentage(),
                        discountRuleLabel(d)))
                .orElse(new DiscountMatch(0, null));
    }
    @Override
    protected void validateForSave(ShoppingCart cart) {
        if (cart.getCustomer() == null || cart.getCustomer().getId() == null) {
            throw new BusinessRuleException("Shopping cart must belong to a customer");
        }
        recalculateTotals(cart);
    }

    private void recalculateTotals(ShoppingCart cart) {
        double subTotal = 0;
        double discountTotal = 0;
        if (cart.getItems() != null) {
            for (CartItem item : cart.getItems()) {
                if (item.getQuantity() <= 0) {
                    throw new BusinessRuleException("Cart item quantity must be positive");
                }
                if (item.getPrice() < 0) {
                    throw new BusinessRuleException("Cart item price cannot be negative");
                }
                PriceBreakdown pricing = breakdownForCartItem(item);
                item.setPrice(round(pricing.amountBeforeDiscount() / item.getQuantity()));
                item.setSubtotal(pricing.amountBeforeDiscount());
                subTotal += item.getSubtotal();
                discountTotal += pricing.discountAmount();
            }
        }

        cart.setDiscount(round(discountTotal));
        if (cart.getDiscount() < 0 || cart.getDiscount() > subTotal) {
            throw new BusinessRuleException("Cart discount must be between zero and subtotal");
        }
        cart.setSubTotal(round(subTotal));
        cart.setTotalAmount(round(subTotal - cart.getDiscount()));
    }

    private void priceCartItem(CartItem item, Integer storeId) {
        Product product = item.getProductVariant().getProduct();
        double baseUnitPrice = product.getBasePrice();
        DiscountMatch discount = resolveDiscount(storeId, product, item.getQuantity());
        PriceBreakdown pricing = priceBreakdown(
                baseUnitPrice, item.getQuantity(), discount.percentage(), hasCustomDesign(item));
        item.setPrice(round(pricing.amountBeforeDiscount() / item.getQuantity()));
        item.setSubtotal(pricing.amountBeforeDiscount());
    }

    private PriceBreakdown breakdownForCartItem(CartItem item) {
        if (item.getProductVariant() == null || item.getProductVariant().getProduct() == null) {
            double fallbackSubtotal = round(item.getPrice() * item.getQuantity());
            return new PriceBreakdown(fallbackSubtotal, 0, 0, fallbackSubtotal, fallbackSubtotal);
        }
        Product product = item.getProductVariant().getProduct();
        Integer storeId = product.getStore() != null ? product.getStore().getId() : null;
        DiscountMatch discount = resolveDiscount(storeId, product, item.getQuantity());
        return priceBreakdown(product.getBasePrice(), item.getQuantity(),
                discount.percentage(), hasCustomDesign(item));
    }

    private PriceBreakdown priceBreakdown(double baseUnitPrice, int quantity,
                                          double discountPercentage,
                                          boolean hasDesign) {
        double baseSubtotal = round(baseUnitPrice * quantity);
        double discountAmount = round(baseSubtotal * discountPercentage / 100);
        double designFeeAmount = hasDesign
                ? round(baseSubtotal * DESIGN_FEE_PERCENTAGE / 100)
                : 0;
        double amountBeforeDiscount = round(baseSubtotal + designFeeAmount);
        double lineTotal = round(amountBeforeDiscount - discountAmount);
        return new PriceBreakdown(baseSubtotal, discountAmount, designFeeAmount,
                amountBeforeDiscount, lineTotal);
    }

    private boolean hasCustomDesign(CartItem item) {
        CustomDesign design = item.getCustomDesign();
        if (design == null) {
            return false;
        }
        return design.getImageUrl() != null && !design.getImageUrl().isBlank();
    }

    private double normalizePercent(Double value, double fallback) {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return fallback;
        }
        return Math.max(0, Math.min(100, value));
    }

    private boolean appliesToQuantity(Discount discount, int quantity) {
        int min = discount.getMinQuantity();
        int max = discount.getMaxQuantity();
        return quantity >= min && (max <= min || quantity <= max);
    }

    private String discountRuleLabel(Discount discount) {
        int min = discount.getMinQuantity();
        int max = discount.getMaxQuantity();
        String range = max <= min ? min + " a mas unidades" : min + "-" + max + " unidades";
        return range + ": -" + round(discount.getDiscountPercentage()) + "%";
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private record DiscountMatch(double percentage, String ruleLabel) {}

    private record PriceBreakdown(double baseSubtotal, double discountAmount,
                                  double designFeeAmount, double amountBeforeDiscount,
                                  double lineTotal) {}
}
