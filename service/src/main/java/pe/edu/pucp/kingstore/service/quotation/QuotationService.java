package pe.edu.pucp.kingstore.service.quotation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.QuotationItem;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.repository.quotation.QuotationRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.domain.model.quotation.QuotationItem;

import pe.edu.pucp.kingstore.domain.dto.quotation.QuotationItemResponseDTO;
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
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new BusinessRuleException("Cart must have at least one item to create a quotation");
        }
        // Un carrito solo puede tener una cotización pendiente activa
        quotationRepository.findByShoppingCartId(cart.getId()).ifPresent(existing -> {
            if (existing.getStatus() == QuotationStatus.PENDING) {
                throw new BusinessRuleException("Cart already has a pending quotation");
            }
        });

        Quotation quotation = new Quotation();
        quotation.setShoppingCart(cart);
        quotation.setStatus(QuotationStatus.PENDING);
        quotation.setDiscount(cart.getDiscount());

        List<QuotationItem> items = cart.getItems().stream().map(cartItem -> {
            QuotationItem qi = new QuotationItem();
            qi.setProductVariant(cartItem.getProductVariant());
            qi.setQuantity(cartItem.getQuantity());
            qi.setPrice(cartItem.getPrice());
            qi.setSubTotal(cartItem.getSubtotal());
            return qi;
        }).toList();

        quotation.setItems(new java.util.ArrayList<>(items));
        return create(quotation);
    }

    /**
     * Devuelve todas las cotizaciones del cliente en una tienda específica.
     */
    @Transactional(readOnly = true)
    public List<Quotation> findByCustomerAndStore(Integer customerId, Integer storeId) {
        requireId(customerId);
        requireId(storeId);
        return quotationRepository.findByShoppingCart_Customer_Id(customerId).stream()
                .filter(q -> {
                    var cart = q.getShoppingCart();
                    if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
                        return false;
                    }
                    return cart.getItems().stream().anyMatch(item -> {
                        var product = item.getProductVariant() != null
                                ? item.getProductVariant().getProduct() : null;
                        return product != null
                                && Objects.equals(product.getStore().getId(), storeId);
                    });
                })
                .toList();
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
        if (status == null || status == QuotationStatus.PENDING) {
            throw new BusinessRuleException("Quotation response must approve or reject the quotation");
        }
        if (status == QuotationStatus.REJECTED && (observations == null || observations.isBlank())) {
            throw new BusinessRuleException("Observations are required when rejecting a quotation");
        }
        Quotation quotation = getById(id);
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
        quotation.setTotalAmount(subTotal - quotation.getDiscount());
    }

    @Transactional(readOnly = true)
    public QuotationResponseDTO toResponseDTO(Quotation quotation, Integer storeId) {
        var customer = quotation.getShoppingCart() != null
                ? quotation.getShoppingCart().getCustomer()
                : null;

        List<QuotationItemResponseDTO> items = quotation.getItems() == null
                ? List.of()
                : quotation.getItems().stream()
                  .map(this::toItemResponseDTO)
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
        dto.setSubTotal(quotation.getSubTotal());
        dto.setDiscount(quotation.getDiscount());
        dto.setTotalAmount(quotation.getTotalAmount());
        dto.setRequestedAt(quotation.getRequestedAt());
        // responseAt es null mientras la cotización está pendiente (se setea al aprobar/rechazar).
        dto.setResponseAt(quotation.getResponseAt());
        dto.setDescription(quotation.getDescription());
        dto.setObservations(quotation.getObservations());
        dto.setStoreId(storeId);
        dto.setItems(items);

        // Datos reales del cliente.
        dto.setCustomerName(MerchantCustomerUtil.customerName(customer));
        dto.setCustomerEmail(MerchantCustomerUtil.customerEmail(customer));
        dto.setCustomerPhone(MerchantCustomerUtil.customerPhone(customer));
        dto.setDocumentType(MerchantCustomerUtil.documentType(customer));
        dto.setDocumentNumber(MerchantCustomerUtil.documentNumber(customer));
        return dto;
    }

    private QuotationItemResponseDTO toItemResponseDTO(QuotationItem item) {
        var variant = item.getProductVariant();
        var product = variant != null ? variant.getProduct() : null;

        String productName = product != null ? product.getName() : null;
        String size = variant != null ? variant.getSize() : null;
        String color = variant != null && variant.getColor() != null ? variant.getColor().name() : null;
        String variantLabel = variant != null
                ? size + " / " + (color != null ? color : "")
                : null;

        QuotationItemResponseDTO dto = new QuotationItemResponseDTO();
        dto.setProductId(product != null ? product.getId() : null);
        dto.setProductName(productName);
        dto.setProductVariantId(variant != null ? variant.getId() : null);
        dto.setSize(size);
        dto.setColor(color);
        dto.setStockAvailable(variant != null ? variant.getStock() : null);
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getPrice());
        dto.setSubTotal(item.getSubTotal());

        // Legacy: el frontend actual usa product/variant/price.
        dto.setProduct(productName);
        dto.setVariant(variantLabel);
        dto.setPrice(item.getPrice());
        return dto;
    }
}
