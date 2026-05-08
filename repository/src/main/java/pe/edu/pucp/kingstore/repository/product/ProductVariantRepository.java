package pe.edu.pucp.kingstore.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;

@Repository
public interface ProductVariantRepository
    extends JpaRepository<ProductVariant, Integer> {

}
