package pe.edu.pucp.kingstore.domain.dto.cart;

import lombok.Data;
import lombok.EqualsAndHashCode;
import pe.edu.pucp.kingstore.domain.dto.BaseEntityDTO;
import pe.edu.pucp.kingstore.domain.dto.product.ProductVariantDTO;

@Data
@EqualsAndHashCode(callSuper = true)
public class CartItemDTO extends BaseEntityDTO {
    private ProductVariantDTO productVariant;
    private int quantity;
    private double price;
    private double subtotal;
}
