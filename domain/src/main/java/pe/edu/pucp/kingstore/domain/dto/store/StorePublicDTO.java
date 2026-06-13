package pe.edu.pucp.kingstore.domain.dto.store;

import lombok.Data;
import pe.edu.pucp.kingstore.domain.model.store.enums.PrimaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.SecondaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.TertiaryColor;

@Data
public class StorePublicDTO {
    private Integer id;
    private String storeName;
    private String slug;
    private String description;
    private String logoUrl;
    private String category;
    private PrimaryColor primaryColor;
    private SecondaryColor secondaryColor;
    private TertiaryColor tertiaryColor;
}
