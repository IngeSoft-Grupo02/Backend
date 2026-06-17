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
