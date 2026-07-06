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
import pe.edu.pucp.kingstore.service.cart.ShoppingCartService;

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

    public QuotationService(QuotationRepository quotationRepository) {
        super(quotationRepository, "Quotation");
        this.quotationRepository = quotationRepository;
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

        List<QuotationItem> items = cart.getItems().stream().map(cartItem -> {
            QuotationItem qi = new QuotationItem();
            qi.setProductVariant(cartItem.getProductVariant());
            qi.setQuantity(cartItem.getQuantity());
            qi.setPrice(cartItem.getPrice());
            qi.setSubTotal(cartItem.getSubtotal());
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

        for (QuotationItem item : quotation.getItems()) {
            boolean hasItemDesign = quotation.getDesigns().stream()
                    .anyMatch(design -> isDesignForItem(design, item)
                            && Boolean.TRUE.equals(design.getActive()));
            double baseSubtotal = baseSubtotal(item);
            double designFeeAmount = hasItemDesign ? designFeeAmount(item) : 0;
            double amountBeforeDiscount = round(baseSubtotal + designFeeAmount);
            item.setSubTotal(amountBeforeDiscount);
            item.setPrice(round(amountBeforeDiscount / item.getQuantity()));
        }
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

        List<QuotationItemResponseDTO> items = quotation.getItems() == null
                ? List.of()
                : quotation.getItems().stream()
                  .map(item -> toItemResponseDTO(item, allDesigns))
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
        dto.setDesignFeeTotal(round(quotation.getSubTotal() - dto.getProductSubtotal()));
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
                                                        List<QuotationDesign> allDesigns) {
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
        dto.setStockAvailable(variant != null ? variant.getStock() : null);
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getPrice());
        dto.setSubTotal(item.getSubTotal());
        applyPricingBreakdown(dto, item, itemDesigns);
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
                                       List<QuotationDesignDTO> itemDesigns) {
        double baseUnitPrice = item.getProductVariant() != null
                && item.getProductVariant().getProduct() != null
                ? item.getProductVariant().getProduct().getBasePrice()
                : item.getPrice();
        double baseSubtotal = round(baseUnitPrice * item.getQuantity());
        boolean hasDesignFee = !itemDesigns.isEmpty();
        double designFeeAmount = hasDesignFee
                ? round(baseSubtotal * ShoppingCartService.DESIGN_FEE_PERCENTAGE / 100)
                : 0;
        double discountAmount = 0;
        double lineTotal = round(item.getSubTotal());

        dto.setBaseUnitPrice(round(baseUnitPrice));
        dto.setBaseSubtotal(baseSubtotal);
        dto.setDiscountAmount(discountAmount);
        dto.setDesignFeeAmount(designFeeAmount);
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

    private double designFeeAmount(QuotationItem item) {
        return round(baseSubtotal(item)
                * ShoppingCartService.DESIGN_FEE_PERCENTAGE / 100);
    }

    private double baseSubtotal(QuotationItem item) {
        double baseUnitPrice = item.getProductVariant() != null
                && item.getProductVariant().getProduct() != null
                ? item.getProductVariant().getProduct().getBasePrice()
                : item.getPrice();
        return round(baseUnitPrice * item.getQuantity());
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
