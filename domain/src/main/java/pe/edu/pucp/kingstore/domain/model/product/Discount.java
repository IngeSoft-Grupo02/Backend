package pe.edu.pucp.kingstore.domain.model.product;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pe.edu.pucp.kingstore.domain.model.BaseEntity;
import pe.edu.pucp.kingstore.domain.model.product.enums.VolumeType;

@Getter
@Setter
@Entity
@Table(name = "discount")
public class Discount extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VolumeType volumeType;

    @Column(nullable = false)
    private int minQuantity;

    @Column(nullable = false)
    private int maxQuantity;

    @Column(nullable = false)
    private double discountPercentage;
}
