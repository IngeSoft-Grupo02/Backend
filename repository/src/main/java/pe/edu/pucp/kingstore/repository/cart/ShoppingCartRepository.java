package pe.edu.pucp.kingstore.repository.cart;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;

import java.util.List;

@Repository
public interface ShoppingCartRepository
    extends JpaRepository<ShoppingCart, Integer> {
    @EntityGraph(attributePaths = {
            "items",
            "items.customDesign",
            "items.productVariant",
            "items.productVariant.product",
            "items.productVariant.product.store"
    })
    List<ShoppingCart> findByCustomerIdAndActiveTrueOrderByIdDesc(Integer customerId);

    @EntityGraph(attributePaths = {
            "items",
            "items.customDesign",
            "items.productVariant",
            "items.productVariant.product",
            "items.productVariant.product.store"
    })
    java.util.Optional<ShoppingCart> findWithItemsById(Integer id);
}
