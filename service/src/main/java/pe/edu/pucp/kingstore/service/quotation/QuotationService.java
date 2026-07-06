package pe.edu.pucp.kingstore.service.quotation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.QuotationDesign;
import pe.edu.pucp.kingstore.domain.model.quotation.QuotationItem;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.repository.quotation.QuotationRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.service.product.StockAvailabilityService;
import pe.edu.pucp.kingstore.service.store.StoreService;

import pe.edu.pucp.kingstore.domain.dto.quotation.QuotationItemResponseDTO;
import pe.edu.pucp.kingstore.domain.dto.quotation.QuotationDesignDTO;
import pe.edu.pucp.kingstore.domain.dto.quotation.QuotationResponseDTO;
import pe.edu.pucp.kingstore.service.user.util.MerchantCustomerUtil;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

@Service
public class QuotationService extends AbstractCrudService<Quotation> {

    private final QuotationRepository quotationRepository;
    private final StockAvailabilityService stockAvailabilityService;

    public QuotationService(QuotationRepository quotationRepository,
                            StockAvailabilityService stockAvailabilityService) {
        super(quotationRepository, "Quotation");
        this.quotationRepository = quotationRepository;
        this.stockAvailabilityService = stockAvailabilityService;
    }
    /**
     * Crea una cotización a partir del carrito del cliente.
     * Copia cada CartItem como QuotationItem para preservar
     * el estado del carrito en el momento de la solicitud.
     */
    @Transactional
    public Quotation createFromCart(ShoppingCart cart) {
        return createFromCart(cart, null);
    }

    @Transactional
    public Quotation createFromCart(ShoppingCart cart, String description) {
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessRuleException("Cart must have at least one item to create a quotation");
        }
        // Un carrito solo puede tener una cotización (unique constraint en shopping_cart_id).
        // Para hacer una nueva cotización el cliente debe usar un carrito nuevo.
        quotationRepository.findByShoppingCartId(cart.getId()).ifPresent(existing -> {
            throw new BusinessRuleException("Cart already has a quotation");
        });

        Quotation quotation = new Quotation();
        quotation.setShoppingCart(cart);
        quotation.setStatus(QuotationStatus.PENDING);
        quotation.setDiscount(0);
        quotation.setDescription(normalizeDescription(description));
        quotation.setDesignFeePercentageApplied(designFeePercentage(cart));
        quotation.setDesignFeeTotal(0.0);

        List<QuotationItem> items = cart.getItems().stream().map(cartItem -> {
            QuotationItem qi = new QuotationItem();
            qi.setProductVariant(cartItem.getProductVariant());
            qi.setQuantity(cartItem.getQuantity());
            qi.setPrice(cartItem.getPrice());
            qi.setSubTotal(cartItem.getSubtotal());
            qi.setSourceCartItemId(cartItem.getId());
            if (cartItem.getCustomDesign() != null
                    && cartItem.getCustomDesign().getDescription() != null
                    && !cartItem.getCustomDesign().getDescription().isBlank()) {
                qi.setCustomerDescription(cartItem.getCustomDesign().getDescription().trim());
            }
            return qi;
        }).toList();

        quotation.setItems(new java.util.ArrayList<>(items));
        return create(quotation);
    }

    @Transactional
    public Quotation applyItemDesignFees(Quotation quotation) {
        if (quotation == null || quotation.getItems() == null || quotation.getItems().isEmpty()) {
            return quotation;
        }
        if (quotation.getDesigns() == null || quotation.getDesigns().isEmpty()) {
            return quotation;
        }

        double appliedPercentage = appliedDesignFeePercentage(quotation);
        quotation.setDesignFeePercentageApplied(appliedPercentage);

        double designFeeTotal = 0;
        for (QuotationItem item : quotation.getItems()) {
            boolean hasItemDesign = quotation.getDesigns().stream()
                    .anyMatch(design -> isDesignForItem(design, item)
                            && Boolean.TRUE.equals(design.getActive()));
            double baseSubtotal = baseSubtotal(item);
            double designFeeAmount = hasItemDesign ? designFeeAmount(item, appliedPercentage) : 0;
            designFeeTotal += designFeeAmount;
            double amountBeforeDiscount = round(baseSubtotal + designFeeAmount);
            item.setSubTotal(amountBeforeDiscount);
            item.setPrice(round(amountBeforeDiscount / item.getQuantity()));
        }
        quotation.setDesignFeeTotal(round(designFeeTotal));
        recalculateTotals(quotation);
        return quotationRepository.save(quotation);
    }

    private String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }

    /**
     * Devuelve todas las cotizaciones del cliente en una tienda específica.
     *
     * La pertenencia a la tienda se determina por el cliente dueño del carrito
     * (customer.store, dato NOT NULL y fiable), NO por los items del carrito.
     * Antes se recorría shopping_cart.items y se exigía product.store == storeId,
     * lo que ocultaba cotizaciones válidas cuando el carrito quedaba vacío o con
     * items históricos inconsistentes — y divergía del criterio del Comerciante,
     * que lista por quotation_item. Ahora ambos ven las mismas cotizaciones
     * válidas del mismo cliente/tienda.
     */
    @Transactional(readOnly = true)
    public List<Quotation> findByCustomerAndStore(Integer customerId, Integer storeId) {
        requireId(customerId);
        requireId(storeId);
        return quotationRepository.findByCustomerIdAndStoreId(customerId, storeId);
    }

    /**
     * Busca una cotización específica del cliente validando scope de tienda.
     */
    @Transactional(readOnly = true)
    public Quotation findByCustomerInStore(Integer quotationId,
                                           Integer customerId, Integer storeId) {
        requireId(quotationId);
        return findByCustomerAndStore(customerId, storeId).stream()
                .filter(q -> Objects.equals(q.getId(), quotationId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Quotation", quotationId));
    }

    /**
     * El cliente acepta la propuesta — pasa a APPROVED.
     */
    @Transactional
    public Quotation acceptByCustomer(Integer quotationId) {
        Quotation quotation = getById(quotationId);
        if (quotation.getStatus() != QuotationStatus.PENDING) {
            throw new BusinessRuleException("Only pending quotations can be accepted by the customer");
        }
        quotation.setStatus(QuotationStatus.APPROVED);
        quotation.setResponseAt(LocalDateTime.now());
        return quotationRepository.save(quotation);
    }

    /**
     * El cliente desiste — pasa a REJECTED.
     */
    @Transactional
    public Quotation declineByCustomer(Integer quotationId) {
        Quotation quotation = getById(quotationId);
        if (quotation.getStatus() != QuotationStatus.PENDING) {
            throw new BusinessRuleException("Only pending quotations can be declined by the customer");
        }
        quotation.setStatus(QuotationStatus.REJECTED);
        quotation.setResponseAt(LocalDateTime.now());
        return quotationRepository.save(quotation);
    }

    @Transactional(readOnly = true)
    public Optional<Quotation> findByShoppingCart(Integer shoppingCartId) {
        requireId(shoppingCartId);
        return quotationRepository.findByShoppingCartId(shoppingCartId);
    }

    @Transactional(readOnly = true)
    public List<Quotation> findByStatus(QuotationStatus status) {
        if (status == null) {
            throw new BusinessRuleException("Quotation status is required");
        }
        return quotationRepository.findByStatus(status);
    }

    @Transactional
    public Quotation respond(Integer id, QuotationStatus status, String observations) {
        return respond(id, status, observations, null);
    }

    @Transactional
    public Quotation respond(Integer id, QuotationStatus status, String observations, Double discountAmount) {
        if (status == null || status == QuotationStatus.PENDING) {
            throw new BusinessRuleException("Quotation response must approve or reject the quotation");
        }
        if (status == QuotationStatus.REJECTED && (observations == null || observations.isBlank())) {
            throw new BusinessRuleException("Observations are required when rejecting a quotation");
        }
        Quotation quotation = getById(id);
        double appliedDiscount = discountAmount == null ? 0 : round(discountAmount);
        if (appliedDiscount < 0 || appliedDiscount > quotation.getSubTotal()) {
            throw new BusinessRuleException("Quotation discount must be between zero and subtotal");
        }
        quotation.setDiscount(appliedDiscount);
        quotation.setTotalAmount(round(quotation.getSubTotal() - appliedDiscount));
        quotation.setStatus(status);
        quotation.setObservations(observations);
        quotation.setResponseAt(LocalDateTime.now());
        return quotationRepository.save(quotation);
    }
    @Transactional(readOnly = true)
    public List<Quotation> findByStoreId(Integer storeId) {
        requireId(storeId);
        return quotationRepository.findByStoreId(storeId);
    }

    @Transactional(readOnly = true)
    public List<Quotation> findByStoreIdAndStatus(Integer storeId, QuotationStatus status) {
        requireId(storeId);
        if (status == null) {
            throw new BusinessRuleException("Quotation status is required");
        }
        return quotationRepository.findByStoreIdAndStatus(storeId, status);
    }

    @Transactional(readOnly = true)
    public Quotation findInStore(Integer quotationId, Integer storeId) {
        requireId(quotationId);
        requireId(storeId);
        return findByStoreId(storeId).stream()
                .filter(q -> Objects.equals(q.getId(), quotationId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Quotation", quotationId));
    }
    @Override
    protected void validateForSave(Quotation quotation) {
        if (quotation.getShoppingCart() == null || quotation.getShoppingCart().getId() == null) {
            throw new BusinessRuleException("Quotation must belong to a shopping cart");
        }
        if (quotation.getStatus() == null) {
            quotation.setStatus(QuotationStatus.PENDING);
        }
        if (quotation.getDesignFeePercentageApplied() == null) {
            quotation.setDesignFeePercentageApplied(designFeePercentage(quotation.getShoppingCart()));
        }
        if (quotation.getDesignFeeTotal() == null) {
            quotation.setDesignFeeTotal(0.0);
        }
        recalculateTotals(quotation);
    }

    private void recalculateTotals(Quotation quotation) {
        double subTotal = 0;
        if (quotation.getItems() != null) {
            for (QuotationItem item : quotation.getItems()) {
                if (item.getQuantity() <= 0) {
                    throw new BusinessRuleException("Quotation item quantity must be positive");
                }
                if (item.getPrice() < 0) {
                    throw new BusinessRuleException("Quotation item price cannot be negative");
                }
                item.setSubTotal(item.getPrice() * item.getQuantity());
                subTotal += item.getSubTotal();
            }
        }

        if (quotation.getDiscount() < 0 || quotation.getDiscount() > subTotal) {
            throw new BusinessRuleException("Quotation discount must be between zero and subtotal");
        }
        quotation.setSubTotal(subTotal);
        quotation.setTotalAmount(round(subTotal - quotation.getDiscount()));
    }

    @Transactional(readOnly = true)
    public QuotationResponseDTO toResponseDTO(Integer quotationId, Integer storeId) {
        requireId(quotationId);
        return toResponseDTO(getById(quotationId), storeId);
    }

    @Transactional(readOnly = true)
    public QuotationResponseDTO toResponseDTO(Quotation quotation, Integer storeId) {
        var customer = quotation.getShoppingCart() != null
                ? quotation.getShoppingCart().getCustomer()
                : null;

        List<QuotationDesign> allDesigns = quotation.getDesigns() == null
                ? List.of()
                : quotation.getDesigns();

        Double appliedPercentageSnapshot = quotation.getDesignFeePercentageApplied();
        List<QuotationItemResponseDTO> items = quotation.getItems() == null
                ? List.of()
                : quotation.getItems().stream()
                  .map(item -> toItemResponseDTO(item, allDesigns, appliedPercentageSnapshot))
                  .toList();

        QuotationResponseDTO dto = new QuotationResponseDTO();
        dto.setId(quotation.getId());
        dto.setCustomer(MerchantCustomerUtil.customerName(customer));
        dto.setStatus(quotation.getStatus());
        dto.setStatusLabel(switch (quotation.getStatus()) {
            case PENDING  -> "Pendiente";
            case APPROVED -> "Aprobada";
            case REJECTED -> "Rechazada";
        });
        double discount = round(quotation.getDiscount());
        double totalAmount = round(quotation.getSubTotal() - discount);
        dto.setSubTotal(quotation.getSubTotal());
        dto.setDiscount(discount);
        dto.setTotalAmount(totalAmount);
        dto.setProductSubtotal(round(items.stream()
                .mapToDouble(QuotationItemResponseDTO::getBaseSubtotal)
                .sum()));
        dto.setDiscountTotal(discount);
        double designFeeTotal = quotation.getDesignFeeTotal() == null
                ? round(quotation.getSubTotal() - dto.getProductSubtotal())
                : round(quotation.getDesignFeeTotal());
        double designFeePercentage = responseDesignFeePercentage(quotation, items);
        dto.setDesignFeeTotal(designFeeTotal);
        dto.setDesignFeePercentage(designFeePercentage);
        dto.setDesignFeePercentageApplied(designFeePercentage);
        dto.setRequestedAt(quotation.getRequestedAt());
        dto.setResponseAt(quotation.getResponseAt());
        dto.setDescription(quotation.getDescription());
        dto.setObservations(quotation.getObservations());
        dto.setStoreId(storeId);
        dto.setItems(items);
        dto.setDesigns(allDesigns.stream()
                    .filter(design -> Boolean.TRUE.equals(design.getActive()))
                    .filter(design -> design.getQuotationItem() == null)
                    .map(this::toDesignDTO)
                    .toList());

        dto.setCustomerName(MerchantCustomerUtil.customerName(customer));
        dto.setCustomerEmail(MerchantCustomerUtil.customerEmail(customer));
        dto.setCustomerPhone(MerchantCustomerUtil.customerPhone(customer));
        dto.setDocumentType(MerchantCustomerUtil.documentType(customer));
        dto.setDocumentNumber(MerchantCustomerUtil.documentNumber(customer));
        return dto;
    }

    private QuotationItemResponseDTO toItemResponseDTO(QuotationItem item,
                                                       List<QuotationDesign> allDesigns,
                                                       Double appliedPercentageSnapshot) {
        var variant = item.getProductVariant();
        var product = variant != null ? variant.getProduct() : null;

        String productName = product != null ? product.getName() : null;
        String size = variant != null ? variant.getSize() : null;
        String color = variant != null && variant.getColor() != null ? variant.getColor().name() : null;
        String variantLabel = variant != null
                ? size + " / " + (color != null ? color : "")
                : null;

        List<QuotationDesignDTO> itemDesigns = allDesigns.stream()
                .filter(d -> isDesignForItem(d, item) && Boolean.TRUE.equals(d.getActive()))
                .map(this::toDesignDTO)
                .toList();

        QuotationItemResponseDTO dto = new QuotationItemResponseDTO();
        dto.setId(item.getId());
        dto.setProductId(product != null ? product.getId() : null);
        dto.setProductName(productName);
        dto.setProductImageUrl(firstProductImageUrl(product));
        dto.setProductVariantId(variant != null ? variant.getId() : null);
        dto.setSize(size);
        dto.setColor(color);
        if (variant != null && variant.getId() != null) {
            var stock = stockAvailabilityService.snapshot(variant.getId());
            dto.setPhysicalStock(stock.physicalStock());
            dto.setReservedStock(stock.reservedStock());
            dto.setStockAvailable(stock.availableStock());
            dto.setStockShortage(stock.shortageFor(item.getQuantity()));
        } else {
            dto.setStockAvailable(null);
            dto.setPhysicalStock(null);
            dto.setReservedStock(null);
            dto.setStockShortage(null);
        }
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getPrice());
        dto.setSubTotal(item.getSubTotal());
        applyPricingBreakdown(dto, item, itemDesigns, appliedPercentageSnapshot);
        dto.setCustomerDescription(item.getCustomerDescription());
        dto.setDesigns(itemDesigns);

        // Legacy: el frontend actual usa product/variant/price.
        dto.setProduct(productName);
        dto.setVariant(variantLabel);
        dto.setPrice(item.getPrice());
        return dto;
    }

    private String firstProductImageUrl(Product product) {
        if (product == null || product.getImageUrls() == null) {
            return null;
        }
        return product.getImageUrls().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(url -> !url.isBlank())
                .findFirst()
                .orElse(null);
    }

    private void applyPricingBreakdown(QuotationItemResponseDTO dto, QuotationItem item,
                                       List<QuotationDesignDTO> itemDesigns,
                                       Double appliedPercentageSnapshot) {
        double baseUnitPrice = item.getProductVariant() != null
                && item.getProductVariant().getProduct() != null
                ? item.getProductVariant().getProduct().getBasePrice()
                : item.getPrice();
        double baseSubtotal = round(baseUnitPrice * item.getQuantity());
        boolean hasDesignFee = !itemDesigns.isEmpty();
        double designFeeAmount = hasDesignFee ? historicalDesignFeeAmount(item, baseSubtotal) : 0;
        double designFeePercentage = appliedPercentageSnapshot == null
                ? (hasDesignFee ? percentageFromAmount(baseSubtotal, designFeeAmount) : designFeePercentage(item))
                : StoreService.effectiveDesignFeePercentage(appliedPercentageSnapshot);
        double discountAmount = 0;
        double lineTotal = round(item.getSubTotal());

        dto.setBaseUnitPrice(round(baseUnitPrice));
        dto.setBaseSubtotal(baseSubtotal);
        dto.setDiscountAmount(discountAmount);
        dto.setDesignFeeAmount(designFeeAmount);
        dto.setDesignFeePercentage(designFeePercentage);
        dto.setLineTotal(lineTotal);
        dto.setHasDesignFee(hasDesignFee);
    }

    private boolean isDesignForItem(QuotationDesign design, QuotationItem item) {
        QuotationItem associatedItem = design.getQuotationItem();
        if (associatedItem == null || item == null) {
            return false;
        }
        if (associatedItem.getId() != null && item.getId() != null) {
            return Objects.equals(associatedItem.getId(), item.getId());
        }
        return associatedItem == item;
    }

    private double designFeeAmount(QuotationItem item, double designFeePercentage) {
        return round(baseSubtotal(item) * designFeePercentage / 100);
    }

    private double baseSubtotal(QuotationItem item) {
        double baseUnitPrice = item.getProductVariant() != null
                && item.getProductVariant().getProduct() != null
                ? item.getProductVariant().getProduct().getBasePrice()
                : item.getPrice();
        return round(baseUnitPrice * item.getQuantity());
    }

    private double designFeePercentage(QuotationItem item) {
        Product product = item.getProductVariant() != null
                ? item.getProductVariant().getProduct()
                : null;
        if (product == null || product.getStore() == null) {
            return StoreService.DEFAULT_DESIGN_FEE_PERCENTAGE;
        }
        return StoreService.effectiveDesignFeePercentage(product.getStore().getDesignFeePercentage());
    }

    private double designFeePercentage(ShoppingCart cart) {
        if (cart == null || cart.getItems() == null) {
            return StoreService.DEFAULT_DESIGN_FEE_PERCENTAGE;
        }
        return cart.getItems().stream()
                .map(item -> item.getProductVariant() != null ? item.getProductVariant().getProduct() : null)
                .filter(Objects::nonNull)
                .map(Product::getStore)
                .filter(Objects::nonNull)
                .map(store -> StoreService.effectiveDesignFeePercentage(store.getDesignFeePercentage()))
                .findFirst()
                .orElse(StoreService.DEFAULT_DESIGN_FEE_PERCENTAGE);
    }

    private double appliedDesignFeePercentage(Quotation quotation) {
        if (quotation == null || quotation.getDesignFeePercentageApplied() == null) {
            return quotation == null
                    ? StoreService.DEFAULT_DESIGN_FEE_PERCENTAGE
                    : designFeePercentage(quotation.getShoppingCart());
        }
        return StoreService.effectiveDesignFeePercentage(quotation.getDesignFeePercentageApplied());
    }

    private double historicalDesignFeeAmount(QuotationItem item, double baseSubtotal) {
        return round(Math.max(0, item.getSubTotal() - baseSubtotal));
    }

    private double percentageFromAmount(double baseSubtotal, double designFeeAmount) {
        if (baseSubtotal <= 0 || designFeeAmount <= 0) {
            return StoreService.DEFAULT_DESIGN_FEE_PERCENTAGE;
        }
        return round(designFeeAmount * 100 / baseSubtotal);
    }

    private double representativeDesignFeePercentage(List<QuotationItemResponseDTO> items) {
        return items.stream()
                .filter(QuotationItemResponseDTO::isHasDesignFee)
                .mapToDouble(QuotationItemResponseDTO::getDesignFeePercentage)
                .findFirst()
                .orElse(StoreService.DEFAULT_DESIGN_FEE_PERCENTAGE);
    }

    private double responseDesignFeePercentage(Quotation quotation, List<QuotationItemResponseDTO> items) {
        if (quotation.getDesignFeePercentageApplied() != null) {
            return StoreService.effectiveDesignFeePercentage(quotation.getDesignFeePercentageApplied());
        }
        return items.stream()
                .filter(QuotationItemResponseDTO::isHasDesignFee)
                .mapToDouble(QuotationItemResponseDTO::getDesignFeePercentage)
                .findFirst()
                .orElseGet(() -> designFeePercentage(quotation.getShoppingCart()));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private QuotationDesignDTO toDesignDTO(QuotationDesign design) {
        return new QuotationDesignDTO(
                design.getId(),
                design.getOriginalFileName(),
                design.getFileUrl(),
                design.getContentType(),
                design.getSizeBytes(),
                design.getQuotationItem() != null ? design.getQuotationItem().getId() : null,
                design.getOverlayX(),
                design.getOverlayY(),
                design.getOverlayWidth(),
                design.getOverlayHeight());
    }
}
