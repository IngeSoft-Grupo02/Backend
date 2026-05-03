package pe.edu.pucp.kingstore.domain.model.cart;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pe.edu.pucp.kingstore.domain.model.BaseEntity;
import pe.edu.pucp.kingstore.domain.model.user.Customer;

import java.util.List;

@Getter
@Setter
@Entity

public class ShoppingCart extends BaseEntity {
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "shopping_cart_id")
    private List<CartItem> items;

    @Column(nullable = false)
    private double subTotal;
    @Column(nullable = false)
    private double discount;
    @Column(nullable = false)
    private double totalAmount;
    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;
}
