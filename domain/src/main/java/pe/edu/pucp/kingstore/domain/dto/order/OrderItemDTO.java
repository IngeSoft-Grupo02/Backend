package pe.edu.pucp.kingstore.domain.dto.order;

import lombok.Data;
import lombok.EqualsAndHashCode;
import pe.edu.pucp.kingstore.domain.dto.BaseEntityDTO;
import pe.edu.pucp.kingstore.domain.dto.product.ProductVariantDTO;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderItemDTO extends BaseEntityDTO {
    private ProductVariantDTO productVariant;
    private Integer quantity;
    private double unitPrice;
    private double subTotal;
}
