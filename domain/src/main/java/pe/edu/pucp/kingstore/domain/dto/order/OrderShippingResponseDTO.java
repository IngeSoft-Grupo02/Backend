package pe.edu.pucp.kingstore.domain.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de respuesta para la dirección/detalle de envío de un pedido.
 * Solo expone los campos que existen físicamente en la entidad ShippingDetail:
 * address, district, description (usada como referencia) y fechas de entrega.
 * No existen en el modelo campos separados de provincia/departamento.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderShippingResponseDTO {
    private String address;
    private String district;
    private String reference;
    private String recipientName;
    private String phone;
    private LocalDate estimatedDeliveryDate;
    private LocalDate actualDeliveryDate;
}
