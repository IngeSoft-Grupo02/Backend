package pe.edu.pucp.kingstore.domain.dto.order;

import lombok.Data;

/**
 * DTO de entrada para cancelar un pedido.
 * El motivo es obligatorio según el PDF (Comerciante 8).
 */
@Data
public class OrderCancelRequestDTO {
    private String reason;
}