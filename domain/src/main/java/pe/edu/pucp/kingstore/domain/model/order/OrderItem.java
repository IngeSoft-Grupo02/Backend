package pe.edu.pucp.kingstore.domain.model.order;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pe.edu.pucp.kingstore.domain.model.BaseEntity;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;

@Getter
@Setter
@Entity
@Table(name = "order_item")
public class OrderItem extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    private Double unitPrice;

    @Column(nullable = false)
    private Double subTotal;
}