package pe.edu.pucp.kingstore.service.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.order.OrderItem;
import pe.edu.pucp.kingstore.domain.model.order.ShippingDetail;
import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;
import pe.edu.pucp.kingstore.repository.order.OrderRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.domain.dto.order.OrderItemResponseDTO;
import pe.edu.pucp.kingstore.domain.dto.order.OrderResponseDTO;
import pe.edu.pucp.kingstore.domain.dto.order.OrderShippingResponseDTO;
import pe.edu.pucp.kingstore.domain.dto.order.ShippingAddressRequestDTO;
import pe.edu.pucp.kingstore.domain.model.order.enums.District;
import pe.edu.pucp.kingstore.service.user.util.MerchantCustomerUtil;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.QuotationItem;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.repository.quotation.QuotationRepository;
import pe.edu.pucp.kingstore.service.store.StoreService;


import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class OrderService extends AbstractCrudService<Order> {

    private final OrderRepository orderRepository;
    private final QuotationRepository quotationRepository;

    public OrderService(OrderRepository orderRepository, QuotationRepository quotationRepository) {
        super(orderRepository, "Order");
        this.orderRepository = orderRepository;
        this.quotationRepository = quotationRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Order> findByQuotation(Integer quotationId) {
        requireId(quotationId);
        return orderRepository.findByQuotationId(quotationId);
    }

    @Transactional(readOnly = true)
    public List<Order> findByStatus(OrderStatus status) {
        if (status == null) {
            throw new BusinessRuleException("Order status is required");
        }
        return orderRepository.findByStatus(status);
    }

    @Transactional
    public int expirePendingPayments() {
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.PENDING_PAYMENT,
                OrderPaymentTimeoutPolicy.expirationCutoff());
        expiredOrders.forEach(order -> order.setStatus(OrderStatus.CANCELLED));
        if (!expiredOrders.isEmpty()) {
            orderRepository.saveAll(expiredOrders);
        }
        return expiredOrders.size();
    }

    @Transactional
    public Order expireIfPaymentTimedOut(Order order) {
        if (!OrderPaymentTimeoutPolicy.isExpired(order)) {
            return order;
        }
        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    @Transactional
    public Order changeStatus(Integer id, OrderStatus status) {
        if (status == null) {
            throw new BusinessRuleException("Order status is required");
        }
        Order order = getById(id);
        order.setStatus(status);
        return orderRepository.save(order);
    }
    @Transactional(readOnly = true)
    public List<Order> findByStoreId(Integer storeId) {
        requireId(storeId);
        return orderRepository.findByStoreId(storeId);
    }

    @Transactional(readOnly = true)
    public List<Order> findByStoreIdAndStatus(Integer storeId, OrderStatus status) {
        requireId(storeId);
        if (status == null) {
            throw new BusinessRuleException("Order status is required");
        }
        return orderRepository.findByStoreIdAndStatus(storeId, status);
    }

    @Transactional(readOnly = true)
    public Order findInStore(Integer orderId, Integer storeId) {
        requireId(orderId);
        requireId(storeId);
        return findByStoreId(storeId).stream()
                .filter(o -> Objects.equals(o.getId(), orderId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }
    @Transactional
    public OrderResponseDTO toResponseDTO(Order order, Integer storeId) {
        order = expireIfPaymentTimedOut(order);
        Double appliedPercentageSnapshot = order.getDesignFeePercentageApplied();
        var quotation = order.getQuotation();
        var customer = quotation != null && quotation.getShoppingCart() != null
                ? quotation.getShoppingCart().getCustomer()
                : null;

        List<OrderItemResponseDTO> itemsDetail = order.getItems() == null
                ? List.of()
                : order.getItems().stream()
                  .map(item -> toItemResponseDTO(item, appliedPercentageSnapshot))
                  .toList();

        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setCustomer(MerchantCustomerUtil.customerName(customer));
        dto.setStatus(order.getStatus());
        dto.setStatusLabel(switch (order.getStatus()) {
            case PENDING_PAYMENT   -> "Pago pendiente";
            case PAYMENT_CONFIRMED -> "Pagado";
            case IN_PREPARATION    -> "En proceso";
            case IN_TRANSIT        -> "Enviado";
            case DELIVERED         -> "Entregado";
            case CANCELLED         -> "Cancelado";
        });
        dto.setItems(order.getItems() == null ? 0 : order.getItems().size());
        dto.setTotal(order.getFinalTotal());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setStoreId(storeId);

        // Detalle real de productos.
        dto.setItemsDetail(itemsDetail);

        // Datos reales del cliente.
        dto.setCustomerName(MerchantCustomerUtil.customerName(customer));
        dto.setCustomerEmail(MerchantCustomerUtil.customerEmail(customer));
        dto.setCustomerPhone(MerchantCustomerUtil.customerPhone(customer));
        dto.setDocumentType(MerchantCustomerUtil.documentType(customer));
        dto.setDocumentNumber(MerchantCustomerUtil.documentNumber(customer));

        // Dirección de envío (null si no hay shipping_detail).
        dto.setShippingDetail(toShippingResponseDTO(order.getShippingDetail()));

        // Montos reales para el comprobante.
        dto.setPartialTotal(order.getPartialTotal());
        dto.setTotalDiscount(order.getTotalDiscount());
        dto.setFinalTotal(order.getFinalTotal());
        dto.setProductSubtotal(round(itemsDetail.stream()
                .mapToDouble(item -> item.getBaseSubtotal() == null ? 0 : item.getBaseSubtotal())
                .sum()));
        double designFeeTotal = order.getDesignFeeTotal() == null
                ? round(itemsDetail.stream()
                    .mapToDouble(item -> item.getDesignFeeAmount() == null ? 0 : item.getDesignFeeAmount())
                    .sum())
                : round(order.getDesignFeeTotal());
        double designFeePercentage = responseDesignFeePercentage(order, itemsDetail);
        dto.setDesignFeeTotal(designFeeTotal);
        dto.setDesignFeePercentage(designFeePercentage);
        dto.setDesignFeePercentageApplied(designFeePercentage);

        // Observaciones provenientes de la cotización asociada (si existen).
        dto.setObservations(quotation != null ? quotation.getObservations() : null);
        return dto;
    }

    private OrderItemResponseDTO toItemResponseDTO(OrderItem item, Double appliedPercentageSnapshot) {
        var variant = item.getProductVariant();
        var product = variant != null ? variant.getProduct() : null;

        OrderItemResponseDTO dto = new OrderItemResponseDTO();
        dto.setProductId(product != null ? product.getId() : null);
        dto.setProductName(product != null ? product.getName() : null);
        dto.setProductVariantId(variant != null ? variant.getId() : null);
        dto.setSize(variant != null ? variant.getSize() : null);
        dto.setColor(variant != null && variant.getColor() != null ? variant.getColor().name() : null);
        dto.setStockAvailable(variant != null ? variant.getStock() : null);
        dto.setQuantity(item.getQuantity());
        dto.setUnitPrice(item.getUnitPrice());
        dto.setSubTotal(item.getSubTotal());
        double baseUnitPrice = product != null ? product.getBasePrice()
                : (item.getUnitPrice() == null ? 0 : item.getUnitPrice());
        double baseSubtotal = round(baseUnitPrice * (item.getQuantity() == null ? 0 : item.getQuantity()));
        double itemSubtotal = item.getSubTotal() == null ? 0 : item.getSubTotal();
        double designFeeAmount = round(Math.max(0, itemSubtotal - baseSubtotal));
        dto.setBaseUnitPrice(round(baseUnitPrice));
        dto.setBaseSubtotal(baseSubtotal);
        dto.setDesignFeeAmount(designFeeAmount);
        dto.setDesignFeePercentage(appliedPercentageSnapshot == null
                ? percentageFromAmount(baseSubtotal, designFeeAmount)
                : StoreService.effectiveDesignFeePercentage(appliedPercentageSnapshot));
        dto.setLineTotal(item.getSubTotal());
        dto.setHasDesignFee(designFeeAmount > 0);
        return dto;
    }

    private double percentageFromAmount(double baseSubtotal, double designFeeAmount) {
        if (baseSubtotal <= 0 || designFeeAmount <= 0) {
            return StoreService.DEFAULT_DESIGN_FEE_PERCENTAGE;
        }
        return round(designFeeAmount * 100 / baseSubtotal);
    }

    private double representativeDesignFeePercentage(List<OrderItemResponseDTO> items) {
        return items.stream()
                .filter(item -> Boolean.TRUE.equals(item.getHasDesignFee()))
                .map(OrderItemResponseDTO::getDesignFeePercentage)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .findFirst()
                .orElse(StoreService.DEFAULT_DESIGN_FEE_PERCENTAGE);
    }

    private OrderShippingResponseDTO toShippingResponseDTO(ShippingDetail shipping) {
        if (shipping == null) {
            return null;
        }
        OrderShippingResponseDTO dto = new OrderShippingResponseDTO();
        dto.setAddress(shipping.getAddress());
        dto.setDistrict(shipping.getDistrict() != null ? shipping.getDistrict().name() : null);
        dto.setReference(shipping.getDescription());
        dto.setRecipientName(shipping.getRecipientName());
        dto.setPhone(shipping.getPhone());
        dto.setEstimatedDeliveryDate(shipping.getEstimatedDeliveryDate());
        dto.setActualDeliveryDate(shipping.getActualDeliveryDate());
        return dto;
    }

    /**
     * Crea un pedido a partir de una cotización aprobada.
     * Copia los items de la cotización al pedido.
     */
    @Transactional
    public Order createFromQuotation(Quotation quotation) {
        if (quotation == null || quotation.getId() == null) {
            throw new BusinessRuleException("Quotation id is required to create an order");
        }
        return createFromQuotation(quotation.getId());
    }

    @Transactional
    public Order createFromQuotation(Integer quotationId) {
        requireId(quotationId);
        Quotation quotation = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation", quotationId));

        if (quotation.getStatus() != QuotationStatus.APPROVED) {
            throw new BusinessRuleException("Order can only be created from an approved quotation");
        }
        Optional<Order> existingOrder = orderRepository.findByQuotationId(quotation.getId());
        if (existingOrder.isPresent()) {
            return existingOrder.get();
        }

        List<QuotationItem> quotationItems = quotation.getItems();
        if (quotationItems == null || quotationItems.isEmpty()) {
            throw new BusinessRuleException("Quotation must have at least one item to create an order");
        }

        Order order = new Order();
        order.setQuotation(quotation);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order.setTotalDiscount(quotation.getDiscount());
        order.setDesignFeeTotal(quotationDesignFeeTotal(quotation));
        order.setDesignFeePercentageApplied(quotationDesignFeePercentage(quotation));

        List<OrderItem> items = quotationItems.stream().map(qi -> {
            if (qi.getProductVariant() == null || qi.getProductVariant().getId() == null) {
                throw new BusinessRuleException("Quotation item must have a product variant to create an order");
            }
            OrderItem oi = new OrderItem();
            oi.setProductVariant(qi.getProductVariant());
            oi.setQuantity(qi.getQuantity());
            oi.setUnitPrice(qi.getPrice());
            oi.setSubTotal(qi.getSubTotal());
            return oi;
        }).toList();

        order.setItems(new java.util.ArrayList<>(items));
        return create(order);
    }

    /**
     * Devuelve los pedidos del cliente en una tienda específica.
     */
    @Transactional(readOnly = true)
    public List<Order> findByCustomerAndStore(Integer customerId, Integer storeId) {
        requireId(customerId);
        requireId(storeId);
        return orderRepository.findByQuotation_ShoppingCart_Customer_Id(customerId).stream()
                .filter(o -> {
                    if (o.getItems() == null || o.getItems().isEmpty()) return false;
                    return o.getItems().stream()
                            .anyMatch(item -> orderItemBelongsToStore(item, storeId));
                })
                .toList();
    }

    /**
     * Determina de forma segura si un ítem del pedido pertenece a la tienda.
     * Tolera datos históricos inconsistentes (variante, producto o tienda en null)
     * sin lanzar NullPointerException.
     */
    private boolean orderItemBelongsToStore(OrderItem item, Integer storeId) {
        if (item == null || item.getProductVariant() == null) {
            return false;
        }
        var product = item.getProductVariant().getProduct();
        if (product == null || product.getStore() == null) {
            return false;
        }
        return Objects.equals(product.getStore().getId(), storeId);
    }

    /**
     * Busca un pedido específico del cliente validando scope de tienda.
     */
    @Transactional(readOnly = true)
    public Order findByCustomerInStore(Integer orderId, Integer customerId, Integer storeId) {
        requireId(orderId);
        return findByCustomerAndStore(customerId, storeId).stream()
                .filter(o -> Objects.equals(o.getId(), orderId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }


    // Mapa de transiciones válidas del flujo merchant
    private static final java.util.Map<OrderStatus, OrderStatus> NEXT_STATUS = java.util.Map.of(
            OrderStatus.PAYMENT_CONFIRMED, OrderStatus.IN_PREPARATION,
            OrderStatus.IN_PREPARATION,    OrderStatus.IN_TRANSIT,
            OrderStatus.IN_TRANSIT,        OrderStatus.DELIVERED
    );

    /**
     * Avanza el pedido al siguiente estado en el flujo.
     * PAYMENT_CONFIRMED → IN_PREPARATION → IN_TRANSIT → DELIVERED
     */
    @Transactional
    public Order advanceStatus(Integer orderId) {
        Order order = getById(orderId);
        OrderStatus next = NEXT_STATUS.get(order.getStatus());
        if (next == null) {
            throw new BusinessRuleException(
                    "Order cannot be advanced from status: " + order.getStatus());
        }
        order.setStatus(next);
        return orderRepository.save(order);
    }

    /**
     * Cancela un pedido. El motivo es obligatorio.
     * Solo se pueden cancelar pedidos que no estén entregados ni ya cancelados.
     */
    @Transactional
    public Order cancel(Integer orderId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("Cancellation reason is required");
        }
        Order order = getById(orderId);
        if (order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleException(
                    "Order cannot be cancelled from status: " + order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLED);
        // Guardamos el motivo en las observaciones de la cotización asociada
        if (order.getQuotation() != null) {
            order.getQuotation().setObservations(reason);
        }
        return orderRepository.save(order);
    }

    @Transactional
    public Order setShippingAddress(Integer orderId, ShippingAddressRequestDTO request) {
        Order order = getById(orderId);
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Cannot set shipping address on a cancelled order");
        }
        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new BusinessRuleException("Cannot set shipping address on a delivered order");
        }

        String address = request.getAddress() != null ? request.getAddress().trim() : "";
        if (address.isEmpty()) {
            throw new BusinessRuleException("Shipping address is required");
        }
        if (address.length() > 255) {
            throw new BusinessRuleException("Shipping address must not exceed 255 characters");
        }

        String districtStr = request.getDistrict() != null ? request.getDistrict().trim() : "";
        if (districtStr.isEmpty()) {
            throw new BusinessRuleException("District is required");
        }
        District district;
        try {
            district = District.valueOf(districtStr.toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Invalid district: " + districtStr);
        }

        String reference = request.getReference() != null ? request.getReference().trim() : null;
        if (reference != null && reference.isEmpty()) {
            reference = null;
        }
        if (reference != null && reference.length() > 500) {
            throw new BusinessRuleException("Reference must not exceed 500 characters");
        }

        String recipientName = request.getRecipientName() != null ? request.getRecipientName().trim() : null;
        if (recipientName != null && recipientName.isEmpty()) {
            recipientName = null;
        }
        if (recipientName != null && recipientName.length() > 150) {
            throw new BusinessRuleException("Recipient name must not exceed 150 characters");
        }

        String phone = request.getPhone() != null ? request.getPhone().trim() : null;
        if (phone != null && phone.isEmpty()) {
            phone = null;
        }
        if (phone != null && phone.length() > 20) {
            throw new BusinessRuleException("Phone must not exceed 20 characters");
        }

        ShippingDetail shipping = order.getShippingDetail();
        if (shipping == null) {
            shipping = new ShippingDetail();
            order.setShippingDetail(shipping);
        }
        shipping.setAddress(address);
        shipping.setDistrict(district);
        shipping.setDescription(reference);
        shipping.setRecipientName(recipientName);
        shipping.setPhone(phone);

        return orderRepository.save(order);
    }

    /**
     * Marca el pedido como enviado e incluye referencia de envío.
     * La referencia es obligatoria (número de guía o nombre del motorizado).
     */
    @Transactional
    public Order ship(Integer orderId, String shippingReference) {
        if (shippingReference == null || shippingReference.isBlank()) {
            throw new BusinessRuleException("Shipping reference is required");
        }
        Order order = getById(orderId);
        if (order.getStatus() != OrderStatus.IN_PREPARATION) {
            throw new BusinessRuleException("Only orders in preparation can be shipped");
        }
        // Guardar referencia en ShippingDetail si existe, o en observaciones
        if (order.getShippingDetail() != null) {
            order.getShippingDetail().setDescription(shippingReference);
        } else {
            if (order.getQuotation() != null) {
                order.getQuotation().setObservations(
                        "Shipping ref: " + shippingReference);
            }
        }
        order.setStatus(OrderStatus.IN_TRANSIT);
        return orderRepository.save(order);
    }

    @Override
    protected void validateForSave(Order order) {
        if (order.getQuotation() == null || order.getQuotation().getId() == null) {
            throw new BusinessRuleException("Order must belong to a quotation");
        }
        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.PENDING_PAYMENT);
        }
        if (order.getDesignFeePercentageApplied() == null && order.getQuotation() != null) {
            order.setDesignFeePercentageApplied(quotationDesignFeePercentage(order.getQuotation()));
        }
        if (order.getDesignFeeTotal() == null && order.getQuotation() != null) {
            order.setDesignFeeTotal(quotationDesignFeeTotal(order.getQuotation()));
        }
        recalculateTotals(order);
    }

    private void recalculateTotals(Order order) {
        double partialTotal = 0;
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    throw new BusinessRuleException("Order item quantity must be positive");
                }
                if (item.getUnitPrice() == null || item.getUnitPrice() < 0) {
                    throw new BusinessRuleException("Order item price cannot be negative");
                }
                item.setSubTotal(round(item.getUnitPrice() * item.getQuantity()));
                partialTotal += item.getSubTotal();
            }
        }

        double totalDiscount = order.getTotalDiscount() == null ? 0 : order.getTotalDiscount();
        if (totalDiscount < 0 || totalDiscount > partialTotal) {
            throw new BusinessRuleException("Order discount must be between zero and partial total");
        }
        double taxableTotal = round(partialTotal - totalDiscount);
        order.setPartialTotal(round(partialTotal));
        order.setTotalDiscount(round(totalDiscount));
        order.setFinalTotal(round(taxableTotal * 1.18));
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private double quotationDesignFeePercentage(Quotation quotation) {
        if (quotation == null) {
            return StoreService.DEFAULT_DESIGN_FEE_PERCENTAGE;
        }
        if (quotation.getDesignFeePercentageApplied() != null) {
            return StoreService.effectiveDesignFeePercentage(quotation.getDesignFeePercentageApplied());
        }
        if (quotation.getItems() == null) {
            return StoreService.DEFAULT_DESIGN_FEE_PERCENTAGE;
        }
        return quotation.getItems().stream()
                .mapToDouble(this::quotationItemDesignFeePercentage)
                .filter(value -> value > 0)
                .findFirst()
                .orElse(StoreService.DEFAULT_DESIGN_FEE_PERCENTAGE);
    }

    private double quotationDesignFeeTotal(Quotation quotation) {
        if (quotation == null) {
            return 0.0;
        }
        if (quotation.getDesignFeeTotal() != null) {
            return round(quotation.getDesignFeeTotal());
        }
        if (quotation.getItems() == null) {
            return 0.0;
        }
        return round(quotation.getItems().stream()
                .mapToDouble(this::quotationItemDesignFeeTotal)
                .sum());
    }

    private double quotationItemDesignFeeTotal(QuotationItem item) {
        var variant = item.getProductVariant();
        var product = variant != null ? variant.getProduct() : null;
        double baseUnitPrice = product != null ? product.getBasePrice() : item.getPrice();
        double baseSubtotal = round(baseUnitPrice * item.getQuantity());
        return round(Math.max(0, item.getSubTotal() - baseSubtotal));
    }

    private double quotationItemDesignFeePercentage(QuotationItem item) {
        var variant = item.getProductVariant();
        var product = variant != null ? variant.getProduct() : null;
        double baseUnitPrice = product != null ? product.getBasePrice() : item.getPrice();
        double baseSubtotal = round(baseUnitPrice * item.getQuantity());
        double designFeeAmount = round(Math.max(0, item.getSubTotal() - baseSubtotal));
        if (baseSubtotal <= 0 || designFeeAmount <= 0) {
            return 0.0;
        }
        return percentageFromAmount(baseSubtotal, designFeeAmount);
    }

    private double responseDesignFeePercentage(Order order, List<OrderItemResponseDTO> itemsDetail) {
        if (order.getDesignFeePercentageApplied() != null) {
            return StoreService.effectiveDesignFeePercentage(order.getDesignFeePercentageApplied());
        }
        if (order.getQuotation() != null && order.getQuotation().getDesignFeePercentageApplied() != null) {
            return StoreService.effectiveDesignFeePercentage(order.getQuotation().getDesignFeePercentageApplied());
        }
        return representativeDesignFeePercentage(itemsDetail);
    }

}
