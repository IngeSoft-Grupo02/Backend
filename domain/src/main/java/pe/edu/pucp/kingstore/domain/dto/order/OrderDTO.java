package pe.edu.pucp.kingstore.domain.dto.order;

import lombok.Data;
import lombok.EqualsAndHashCode;
import pe.edu.pucp.kingstore.domain.dto.BaseEntityDTO;
import pe.edu.pucp.kingstore.domain.dto.quotation.QuotationDTO;
import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class OrderDTO extends BaseEntityDTO {
    private QuotationDTO quotation;
    private List<OrderItemDTO> items;
    private ShippingDetailDTO shippingDetail;
    private LocalDateTime createdAt;
    private LocalDateTime partialTotal;
    private Double totalDiscount;
    private OrderStatus status;
}
