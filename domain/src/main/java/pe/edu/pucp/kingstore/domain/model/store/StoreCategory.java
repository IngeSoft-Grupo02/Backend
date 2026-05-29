package pe.edu.pucp.kingstore.domain.model.store;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import pe.edu.pucp.kingstore.domain.model.BaseEntity;

@Entity
@Table(name = "store_category")
@Getter
@Setter
public class StoreCategory extends BaseEntity {

    @Column(nullable = false, unique = true, length = 50, name = "store_category_name")
    private String storeCategoryName;
}
