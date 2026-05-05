package pe.edu.pucp.kingstore.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository
    extends JpaRepository<Order, Integer> {
    Optional<Order> findByQuotationId(Integer quotationId);
    List<Order> findByStatus(OrderStatus status);

}
