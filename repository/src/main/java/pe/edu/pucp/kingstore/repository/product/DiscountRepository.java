package pe.edu.pucp.kingstore.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.product.Discount;

import java.util.List;

@Repository
public interface DiscountRepository
    extends JpaRepository<Discount, Integer> {

    List<Discount> findByProductId(Integer productId);

}
