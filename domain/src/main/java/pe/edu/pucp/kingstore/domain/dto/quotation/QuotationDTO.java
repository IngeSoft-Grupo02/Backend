package pe.edu.pucp.kingstore.domain.dto.quotation;

import lombok.Data;
import lombok.EqualsAndHashCode;
import pe.edu.pucp.kingstore.domain.dto.BaseEntityDTO;
import pe.edu.pucp.kingstore.domain.dto.cart.ShoppingCartDTO;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class QuotationDTO extends BaseEntityDTO {
    private ShoppingCartDTO shoppingCart;
    private List<QuotationItemDTO> items;
    private double subTotal;
    private double discount;
    private double totalAmount;
    private QuotationStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime responseAt;
    private String Description;
    private String observation;
}
