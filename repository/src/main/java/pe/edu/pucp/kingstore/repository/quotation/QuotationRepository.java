package pe.edu.pucp.kingstore.repository.quotation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.domain.model.user.Customer;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuotationRepository
    extends JpaRepository<Quotation, Integer> {

    Optional<Quotation> findByShoppingCartId(Integer shoppingCartId);

    List<Quotation> findByStatus(QuotationStatus status);

    @Query("""
            select distinct q
            from Quotation q
            join q.items i,
            Product p
            join p.variants v
            where p.store.id = :storeId
              and v = i.productVariant
            """)
    List<Quotation> findByStoreId(@Param("storeId") Integer storeId);

    @Query("""
            select distinct q
            from Quotation q
            join q.items i,
            Product p
            join p.variants v
            where p.store.id = :storeId
              and v = i.productVariant
              and q.status = :status
            """)
    List<Quotation> findByStoreIdAndStatus(@Param("storeId") Integer storeId,
                                           @Param("status") QuotationStatus status);

    List<Quotation> findByShoppingCart_Customer_Id(Integer customerId);

    /**
     * Lista las cotizaciones de un cliente dentro de una tienda determinando la
     * pertenencia por el cliente dueño del carrito y su tienda (customer.store),
     * que es un dato fiable y NOT NULL. No depende de los items del carrito
     * (cart_item), por lo que no oculta cotizaciones válidas cuando el carrito
     * quedó vacío o con items históricos inconsistentes.
     */
    @Query("""
            select distinct q
            from Quotation q
            where q.shoppingCart.customer.id = :customerId
              and q.shoppingCart.customer.store.id = :storeId
            """)
    List<Quotation> findByCustomerIdAndStoreId(@Param("customerId") Integer customerId,
                                               @Param("storeId") Integer storeId);
}