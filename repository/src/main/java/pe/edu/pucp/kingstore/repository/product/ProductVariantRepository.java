package pe.edu.pucp.kingstore.repository.product;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import java.util.Optional;

@Repository
public interface ProductVariantRepository
    extends JpaRepository<ProductVariant, Integer> {
    @EntityGraph(attributePaths = {"product", "product.store"})
    Optional<ProductVariant> findWithProductById(Integer id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select v
            from ProductVariant v
            left join fetch v.product p
            left join fetch p.store
            where v.id = :id
            """)
    Optional<ProductVariant> findWithProductByIdForUpdate(@Param("id") Integer id);
}
