package pe.edu.pucp.kingstore.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.product.Discount;

import java.util.List;

@Repository
public interface DiscountRepository
    extends JpaRepository<Discount, Integer> {

    List<Discount> findByProductId(Integer productId);
    List<Discount> findByProductStoreId(Integer storeId);

    @Query("""
            select d
            from Discount d
            left join d.product p
            left join d.store s
            where s.id = :storeId
               or p.store.id = :storeId
            order by d.id asc
            """)
    List<Discount> findByStoreId(@Param("storeId") Integer storeId);

}
