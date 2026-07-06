package pe.edu.pucp.kingstore.domain.dto.store;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import pe.edu.pucp.kingstore.domain.model.store.enums.PrimaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.SecondaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.TertiaryColor;

/**
 * DTO para crear o actualizar una tienda desde el panel del comerciante.
 */
@Data
public class MerchantStoreRequestDTO {
    private String name;
    private String slug;
    private String description;
    private PrimaryColor primaryColor;
    private SecondaryColor secondaryColor;
    private TertiaryColor tertiaryColor;
    private Integer categoryId;
    private String logoUrl;
    private String status;
    @JsonAlias({"customizationIncrement", "customizationFeePercentage"})
    private Double designFeePercentage;
}
