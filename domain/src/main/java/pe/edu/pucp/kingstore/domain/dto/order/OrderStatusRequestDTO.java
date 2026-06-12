package pe.edu.pucp.kingstore.domain.dto.order;


import lombok.Data;
import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;

/**
 * DTO de solicitud para cambiar el estado de una orden.
 */
@Data
public class OrderStatusRequestDTO {
    private OrderStatus status;
}