package pe.edu.pucp.kingstore.domain.dto.cart;

import lombok.Data;
import lombok.EqualsAndHashCode;
import pe.edu.pucp.kingstore.domain.dto.BaseEntityDTO;
import pe.edu.pucp.kingstore.domain.dto.user.CustomerDTO;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ShoppingCartDTO extends BaseEntityDTO {
    private List<CartItemDTO> items;
    private double subtotal;
    private double discount;
    private double totalAmount;
    private Integer customerId; // paso solo ID para identificar luego Customer
}
