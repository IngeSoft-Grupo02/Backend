package pe.edu.pucp.kingstore.domain.dto.product;

import lombok.Data;
import lombok.EqualsAndHashCode;
import pe.edu.pucp.kingstore.domain.dto.BaseEntityDTO;
import pe.edu.pucp.kingstore.domain.model.product.enums.VolumeType;

@Data
@EqualsAndHashCode(callSuper = true)
public class DiscountDTO extends BaseEntityDTO {
    private Integer productId;
    private VolumeType volumeType; //sintaxis de nombre de varible corregida
    private int minQuantity;
    private int maxQuantity;
    private double discountPercentage;
}
