package pe.edu.pucp.kingstore.domain.model.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import pe.edu.pucp.kingstore.domain.model.BaseEntity;

@Getter
@Setter
@Entity
@Table(name = "attribute")
public class Attribute extends BaseEntity {
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 100)
    private String value;
}
