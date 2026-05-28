package pe.edu.pucp.kingstore.domain.dto.product;

import lombok.Data;
import lombok.EqualsAndHashCode;
import pe.edu.pucp.kingstore.domain.dto.BaseEntityDTO;

@Data
@EqualsAndHashCode(callSuper = true)
public class AttributeDTO extends BaseEntityDTO {
    private String name;
    private String value;
}
