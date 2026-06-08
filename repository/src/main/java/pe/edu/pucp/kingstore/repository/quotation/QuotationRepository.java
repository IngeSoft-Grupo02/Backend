package pe.edu.pucp.kingstore.repository.quotation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;


import java.util.List;
import java.util.Optional;

@Repository
public interface QuotationRepository
    extends JpaRepository<Quotation, Integer> {

    Optional<Quotation> findByShoppingCartId(Integer shoppingCartId);
    List<Quotation> findByStatus (QuotationStatus status);

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

}
