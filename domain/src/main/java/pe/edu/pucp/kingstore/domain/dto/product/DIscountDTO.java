package pe.edu.pucp.kingstore.domain.dto.product;

import lombok.Data;
import lombok.EqualsAndHashCode;
import pe.edu.pucp.kingstore.domain.dto.BaseEntityDTO;
import pe.edu.pucp.kingstore.domain.model.product.enums.VolumeType;

@Data
@EqualsAndHashCode(callSuper = true)
public class DIscountDTO extends BaseEntityDTO {
    private ProductDTO product;
    private VolumeType volumeType;

}
