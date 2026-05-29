package pe.edu.pucp.kingstore.domain.dto.store;

import lombok.Data;
import lombok.EqualsAndHashCode;
import pe.edu.pucp.kingstore.domain.dto.BaseEntityDTO;
import pe.edu.pucp.kingstore.domain.model.store.enums.PrimaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.SecondaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.TertiaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class StoreDTO extends BaseEntityDTO {
    private Integer merchantId;
    private Integer categoryId;
    private String storeName;
    private String slug;
    private String description;
    private String logoUrl;
    private LocalDateTime createdAt;
    private PrimaryColor primaryColor;
    private SecondaryColor secondaryColor;
    private TertiaryColor tertiaryColor;
    private StoreStatus storeStatus;
}
