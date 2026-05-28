package pe.edu.pucp.kingstore.domain.dto.store;

import lombok.Data;
import lombok.EqualsAndHashCode;
import pe.edu.pucp.kingstore.domain.dto.BaseEntityDTO;
import pe.edu.pucp.kingstore.domain.model.store.enums.ColorPalette;
import pe.edu.pucp.kingstore.domain.model.store.enums.CustomerGender;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreCategory;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class StoreDTO extends BaseEntityDTO {
    private Integer merchantId;
    private String storeName;
    private String slug;
    private String description;
    private String logoUrl;
    private String primaryColor;
    private String secondaryColor;
    private LocalDateTime createdAt;
    private List<StoreCategory> categories;
    private List<CustomerGender> genders;
    private ColorPalette colorPalette;
}
