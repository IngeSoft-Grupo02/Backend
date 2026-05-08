package pe.edu.pucp.kingstore.service.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.order.OrderItem;
import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;
import pe.edu.pucp.kingstore.repository.order.OrderRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

import java.util.List;
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

    @Override
    protected void validateForSave(Order order) {
        if (order.getQuotation() == null || order.getQuotation().getId() == null) {
            throw new BusinessRuleException("Order must belong to a quotation");
        }
        if (order.getStatus() == null) {
            order.setStatus(OrderStatus.REGISTERED);
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
