package pe.edu.pucp.kingstore.repository.cart;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;

import java.util.List;
import java.util.Optional;
@Repository
public interface ShoppingCartRepository
    extends JpaRepository<ShoppingCart, Integer> {
    Optional<ShoppingCart> findByCustomerId(Integer customerId);

}
