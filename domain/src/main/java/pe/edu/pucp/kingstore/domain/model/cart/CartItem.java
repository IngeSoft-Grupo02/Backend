package pe.edu.pucp.kingstore.domain.model.cart;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pe.edu.pucp.kingstore.domain.model.BaseEntity;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;

@Getter
@Setter
@Entity
@Table(name = "cart_item")
public class CartItem extends BaseEntity {
    @ManyToOne
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private double subtotal;

}
