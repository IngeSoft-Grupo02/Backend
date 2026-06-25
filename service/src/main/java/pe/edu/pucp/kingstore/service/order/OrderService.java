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
import pe.edu.pucp.kingstore.service.user.util.MerchantCustomerUtil;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;


import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class OrderService extends AbstractCrudService<Order> {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        super(orderRepository, "Order");
        this.orderRepository = orderRepository;
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
    @Transactional(readOnly = true)
    public OrderResponseDTO toResponseDTO(Order order, Integer storeId) {
        var quotation = order.getQuotation();
        var customer = quotation != null && quotation.getShoppingCart() != null
                ? quotation.getShoppingCart().getCustomer()
                : null;

        List<OrderItemResponseDTO> itemsDetail = order.getItems() == null
                ? List.of()
                : order.getItems().stream()
                  .map(this::toItemResponseDTO)
                  .toList();

        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setId(order.getId());
        dto.setCustomer(MerchantCustomerUtil.customerName(customer));
        dto.setStatus(order.getStatus());
        dto.setStatusLabel(switch (order.getStatus()) {
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

        // Observaciones provenientes de la cotización asociada (si existen).
        dto.setObservations(quotation != null ? quotation.getObservations() : null);
        return dto;
    }

    private OrderItemResponseDTO toItemResponseDTO(OrderItem item) {
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
        return dto;
    }

    private OrderShippingResponseDTO toShippingResponseDTO(ShippingDetail shipping) {
        if (shipping == null) {
            return null;
        }
        OrderShippingResponseDTO dto = new OrderShippingResponseDTO();
        dto.setAddress(shipping.getAddress());
        dto.setDistrict(shipping.getDistrict() != null ? shipping.getDistrict().name() : null);
        dto.setReference(shipping.getDescription());
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
        if (quotation.getStatus() != pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus.APPROVED) {
            throw new BusinessRuleException("Order can only be created from an approved quotation");
        }
        // Verificar que no existe ya un pedido para esta cotización
        orderRepository.findByQuotationId(quotation.getId()).ifPresent(existing -> {
            throw new BusinessRuleException("Order already exists for this quotation");
        });

        Order order = new Order();
        order.setQuotation(quotation);
        order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
        order.setTotalDiscount(quotation.getDiscount());

        List<OrderItem> items = quotation.getItems().stream().map(qi -> {
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
                    return o.getItems().stream().anyMatch(item -> {
                        var product = item.getProductVariant() != null
                                ? item.getProductVariant().getProduct() : null;
                        return product != null && Objects.equals(product.getStore().getId(), storeId);
                    });
                })
                .toList();
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
            order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
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
                item.setSubTotal(item.getUnitPrice() * item.getQuantity());
                partialTotal += item.getSubTotal();
            }
        }

        double totalDiscount = order.getTotalDiscount() == null ? 0 : order.getTotalDiscount();
        if (totalDiscount < 0 || totalDiscount > partialTotal) {
            throw new BusinessRuleException("Order discount must be between zero and partial total");
        }
        order.setPartialTotal(partialTotal);
        order.setTotalDiscount(totalDiscount);
        order.setFinalTotal(partialTotal - totalDiscount);
    }

}
