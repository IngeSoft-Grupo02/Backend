package pe.edu.pucp.kingstore.service.cart;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.cart.CartItem;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.repository.cart.ShoppingCartRepository;
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
import pe.edu.pucp.kingstore.domain.model.product.Product;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class ShoppingCartService extends AbstractCrudService<ShoppingCart> {

    private final ShoppingCartRepository shoppingCartRepository;
    private final DiscountRepository discountRepository;

    public ShoppingCartService(ShoppingCartRepository shoppingCartRepository,
                               DiscountRepository discountRepository) {
        super(shoppingCartRepository, "Shopping cart");
        this.shoppingCartRepository = shoppingCartRepository;
        this.discountRepository = discountRepository;
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
     * Obtiene el carrito activo del cliente o crea uno vacío si no existe.
     */
    @Transactional
    public ShoppingCart getOrCreateCart(Customer customer) {
        return shoppingCartRepository.findByCustomerIdAndActiveTrueOrderByIdDesc(customer.getId())
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    ShoppingCart cart = new ShoppingCart();
                    cart.setCustomer(customer);
                    cart.setItems(new ArrayList<>());
                    cart.setSubTotal(0);
                    cart.setDiscount(0);
                    cart.setTotalAmount(0);
                    return shoppingCartRepository.save(cart);
                });
    }

    /**
     * Agrega una variante al carrito. Si ya existe un item con esa variante,
     * suma la cantidad en lugar de crear un duplicado.
     * Calcula el descuento por volumen aplicable automáticamente.
     */
    @Transactional
    public ShoppingCart addItem(ShoppingCart cart, ProductVariant variant,
                                int quantity, Integer storeId) {
        if (quantity <= 0) {
            throw new BusinessRuleException("Quantity must be positive");
        }
        if (variant.getStock() < quantity) {
            throw new BusinessRuleException("Not enough stock for the requested quantity");
        }

        // Si ya existe ese variant en el carrito, suma la cantidad
        CartItem existing = cart.getItems().stream()
                .filter(i -> Objects.equals(i.getProductVariant().getId(), variant.getId()))
                .findFirst()
                .orElse(null);

        double price = variant.getProduct().getBasePrice();
        double discountApplied = resolveDiscount(storeId, variant.getProduct(), quantity);
        double finalPrice = price * (1 - discountApplied / 100);

        if (existing != null) {
            int newQty = existing.getQuantity() + quantity;
            existing.setQuantity(newQty);
            existing.setPrice(finalPrice);
            existing.setSubtotal(finalPrice * newQty);
        } else {
            CartItem item = new CartItem();
            item.setProductVariant(variant);
            item.setQuantity(quantity);
            item.setPrice(finalPrice);
            item.setSubtotal(finalPrice * quantity);
            cart.getItems().add(item);
        }

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
        if (variant.getStock() < quantity) {
            throw new BusinessRuleException("Not enough stock for the requested quantity");
        }

        double price = variant.getProduct().getBasePrice();
        double discountApplied = resolveDiscount(storeId, variant.getProduct(), quantity);
        double finalPrice = price * (1 - discountApplied / 100);

        item.setQuantity(quantity);
        item.setPrice(finalPrice);
        item.setSubtotal(finalPrice * quantity);

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
        design.setProduct(item.getProductVariant().getProduct());

        item.setCustomDesign(design);

        return shoppingCartRepository.save(cart);
    }
// ── Entity → DTO ──────────────────────────────────────────────────────────────

    public CartResponseDTO toResponseDTO(ShoppingCart cart) {
        List<CartResponseDTO.CartItemResponseDTO> items = cart.getItems() == null
                ? List.of()
                : cart.getItems().stream().map(item -> {
            ProductVariant variant = item.getProductVariant();
            Product product = variant.getProduct();
            double originalPrice = product.getBasePrice();
            double discountApplied = originalPrice > 0
                    ? (1 - item.getPrice() / originalPrice) * 100 : 0;
            CartResponseDTO.CustomDesignResponseDTO designDTO = null;
            if (item.getCustomDesign() != null) {
                CustomDesign d = item.getCustomDesign();
                designDTO = new CartResponseDTO.CustomDesignResponseDTO(
                        d.getId(),
                        d.getImageUrl(),
                        d.getDescription(),
                        d.getObservations(),
                        d.getSentAt()
                );
            }
            return new CartResponseDTO.CartItemResponseDTO(
                    item.getId(),
                    variant.getId(),
                    product.getName(),
                    variant.getSize(),
                    variant.getColor(),
                    item.getPrice(),
                    item.getQuantity(),
                    item.getSubtotal(),
                    Math.round(discountApplied * 100.0) / 100.0,
                    designDTO
            );
        }).toList();

        return new CartResponseDTO(
                cart.getId(),
                items,
                cart.getSubTotal(),
                cart.getDiscount(),
                cart.getTotalAmount()
        );
    }

// ── Helpers privados ──────────────────────────────────────────────────────────

    /**
     * Busca el descuento por volumen activo más beneficioso para el producto
     * según la cantidad solicitada. Retorna el porcentaje a aplicar (0 si ninguno aplica).
     */
    private double resolveDiscount(Integer storeId, Product product, int quantity) {
        return discountRepository.findByStoreId(storeId).stream()
                .filter(d -> Boolean.TRUE.equals(d.getActive()))
                .filter(d -> d.getProduct() == null
                        || Objects.equals(d.getProduct().getId(), product.getId()))
                .filter(d -> quantity >= d.getMinQuantity()
                        && quantity <= d.getMaxQuantity())
                .mapToDouble(Discount::getDiscountPercentage)
                .max()
                .orElse(0);
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
        if (cart.getItems() != null) {
            for (CartItem item : cart.getItems()) {
                if (item.getQuantity() <= 0) {
                    throw new BusinessRuleException("Cart item quantity must be positive");
                }
                if (item.getPrice() < 0) {
                    throw new BusinessRuleException("Cart item price cannot be negative");
                }
                item.setSubtotal(item.getPrice() * item.getQuantity());
                subTotal += item.getSubtotal();
            }
        }

        if (cart.getDiscount() < 0 || cart.getDiscount() > subTotal) {
            throw new BusinessRuleException("Cart discount must be between zero and subtotal");
        }
        cart.setSubTotal(subTotal);
        cart.setTotalAmount(subTotal - cart.getDiscount());
    }
}
