package pe.edu.pucp.kingstore.repository.product;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import java.util.Optional;

@Repository
public interface ProductVariantRepository
    extends JpaRepository<ProductVariant, Integer> {
    @EntityGraph(attributePaths = {"product", "product.store"})
    Optional<ProductVariant> findWithProductById(Integer id);
}
