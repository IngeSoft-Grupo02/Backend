package pe.edu.pucp.kingstore.repository.order;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository
    extends JpaRepository<Order, Integer> {
    Optional<Order> findByQuotationId(Integer quotationId);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime createdAt);

    @Query("""
            select distinct o
            from Order o
            join o.items i,
            Product p
            join p.variants v
            where p.store.id = :storeId
              and v = i.productVariant
            """)
    List<Order> findByStoreId(@Param("storeId") Integer storeId);

    @Query("""
            select distinct o
            from Order o
            join o.items i,
            Product p
            join p.variants v
            where p.store.id = :storeId
              and v = i.productVariant
              and o.status = :status
            """)
    List<Order> findByStoreIdAndStatus(@Param("storeId") Integer storeId,
                                       @Param("status") OrderStatus status);
    List<Order> findByQuotation_ShoppingCart_Customer_Id(Integer customerId);

}
