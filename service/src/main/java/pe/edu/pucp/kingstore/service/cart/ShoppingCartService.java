package pe.edu.pucp.kingstore.service.cart;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.cart.CartItem;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.repository.cart.ShoppingCartRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

import java.util.Optional;

@Service
public class ShoppingCartService extends AbstractCrudService<ShoppingCart> {

    private final ShoppingCartRepository shoppingCartRepository;

    public ShoppingCartService(ShoppingCartRepository shoppingCartRepository) {
        super(shoppingCartRepository, "Shopping cart");
        this.shoppingCartRepository = shoppingCartRepository;
    }

    @Transactional(readOnly = true)
    public Optional<ShoppingCart> findByCustomer(Integer customerId) {
        requireId(customerId);
        return shoppingCartRepository.findByCustomerId(customerId);
    }

    @Override
    protected void validateForSave(ShoppingCart cart) {
        if (cart.getCustomer() == null || cart.getCustomer().getId() == null) {
            throw new BusinessRuleException("Shopping cart must belong to a customer");
        }
        recalculateTotals(cart);
    }

    private void recalculateTotals(ShoppingCart cart) {
        double subTotal = 0;
        if (cart.getItems() != null) {
            for (CartItem item : cart.getItems()) {
                if (item.getQuantity() <= 0) {
                    throw new BusinessRuleException("Cart item quantity must be positive");
                }
                if (item.getPrice() < 0) {
                    throw new BusinessRuleException("Cart item price cannot be negative");
                }
                item.setSubtotal(item.getPrice() * item.getQuantity());
                subTotal += item.getSubtotal();
            }
        }

        if (cart.getDiscount() < 0 || cart.getDiscount() > subTotal) {
            throw new BusinessRuleException("Cart discount must be between zero and subtotal");
        }
        cart.setSubTotal(subTotal);
        cart.setTotalAmount(subTotal - cart.getDiscount());
    }
}
