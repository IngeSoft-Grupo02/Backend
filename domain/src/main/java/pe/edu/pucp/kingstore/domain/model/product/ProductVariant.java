package pe.edu.pucp.kingstore.domain.model.product;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Column(nullable = false)
    private String size;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Color color;
    @Column(nullable = false)
    private int stock;

    /**
     * Navegación de solo lectura hacia el producto dueño de la variante.
     * La FK product_id ya existe en la tabla product_variant y es escrita por
     * la relación Product.variants (@OneToMany @JoinColumn). Aquí solo se lee,
     * por eso insertable=false/updatable=false, para no romper la persistencia.
     */
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;
}
