package pe.edu.pucp.kingstore.domain.model.product;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import pe.edu.pucp.kingstore.domain.model.BaseEntity;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import pe.edu.pucp.kingstore.domain.model.product.enums.Size;

@Getter
@Setter
@Entity
@Table(name = "product_variant")
public class ProductVariant extends BaseEntity {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Size size;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Color color;
    @Column(nullable = false)
    private int stock;
}
