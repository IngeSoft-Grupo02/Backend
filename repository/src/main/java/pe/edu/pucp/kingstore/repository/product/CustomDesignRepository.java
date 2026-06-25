package pe.edu.pucp.kingstore.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.product.CustomDesign;

import java.util.List;

@Repository
public interface CustomDesignRepository extends JpaRepository<CustomDesign, Integer> {
    List<CustomDesign> findByProduct_Store_Id(Integer storeId);
    List<CustomDesign> findByProduct_Id(Integer productId);
}