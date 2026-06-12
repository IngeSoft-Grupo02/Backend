package pe.edu.pucp.kingstore.domain.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para órdenes en el panel del comerciante.
 * Contiene datos calculados como statusLabel y customerName
 * que no forman parte de la entidad Order.
 */
@Data
@AllArgsConstructor
public class OrderResponseDTO {
    private Integer id;
    private String customer;
    private OrderStatus status;
    private String statusLabel;
    private int items;
    private Double total;
    private LocalDateTime createdAt;
    private Integer storeId;
}