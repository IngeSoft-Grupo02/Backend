package pe.edu.pucp.kingstore.domain.dto.order;

import lombok.Data;

/**
 * DTO de entrada para marcar un pedido como enviado.
 * Requiere número de guía o nombre del motorizado.
 */
@Data
public class OrderShipRequestDTO {
    private String shippingReference;
}