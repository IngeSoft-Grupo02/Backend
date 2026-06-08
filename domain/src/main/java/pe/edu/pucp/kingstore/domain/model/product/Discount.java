package pe.edu.pucp.kingstore.domain.model.product;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pe.edu.pucp.kingstore.domain.model.BaseEntity;
import pe.edu.pucp.kingstore.domain.model.product.enums.VolumeType;
import pe.edu.pucp.kingstore.domain.model.store.Store;

@Getter
@Setter
@Entity
@Table(name = "discount")
public class Discount extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "store_id")
    private Store store;

    @Column(length = 150)
    private String name;

    @Column(length = 30)
    private String discountType;

    @Column(length = 100)
    private String appliesTo;

    @Column(nullable = false)
    private int usageCount = 0;

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
