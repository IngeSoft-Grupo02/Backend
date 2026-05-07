package pe.edu.pucp.kingstore.domain.dto.product;

import lombok.Data;
import lombok.EqualsAndHashCode;
import pe.edu.pucp.kingstore.domain.dto.BaseEntityDTO;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import pe.edu.pucp.kingstore.domain.model.product.enums.Size;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductVariantDTO extends BaseEntityDTO {
    private Size size;
    private Color color;
    private int stock;
}
