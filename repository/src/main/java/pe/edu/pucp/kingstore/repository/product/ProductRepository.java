package pe.edu.pucp.kingstore.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.product.Product;

import java.util.List;

@Repository
public interface ProductRepository
    extends JpaRepository<Product, Integer> {
    List<Product> findByStoreId(Integer storeId);
    List<Product> findByStoreIdAndActive(Integer storeId, Boolean active);
    List<Product> findByNameContainingAndStoreId(String name, Integer storeId);

}
