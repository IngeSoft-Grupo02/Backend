package pe.edu.pucp.kingstore.domain.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta para órdenes en el panel del comerciante.
 * Contiene datos calculados como statusLabel y customerName
 * que no forman parte de la entidad Order.
 *
 * Campos legacy mantenidos por compatibilidad con el frontend actual:
 * customer, status, statusLabel, items (cantidad), total, createdAt, storeId.
 */
@Data
@NoArgsConstructor
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

    // Detalle real de productos del pedido.
    private List<OrderItemResponseDTO> itemsDetail;

    // Datos reales del cliente (order -> quotation -> shoppingCart -> customer).
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String documentType;
    private String documentNumber;

    // Dirección/detalle de envío (null si el pedido no tiene shipping_detail).
    private OrderShippingResponseDTO shippingDetail;

    // Montos reales para el comprobante.
    private Double partialTotal;
    private Double totalDiscount;
    private Double finalTotal;

    // Observaciones provenientes de la cotización asociada (si existen).
    private String observations;
}
