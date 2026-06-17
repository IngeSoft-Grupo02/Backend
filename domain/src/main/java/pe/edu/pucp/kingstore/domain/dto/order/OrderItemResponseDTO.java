package pe.edu.pucp.kingstore.domain.dto.order;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para ítems de pedido en el panel del comerciante.
 * Expone el detalle real por producto/variante para guía de despacho y comprobante.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDTO {
    private Integer productId;
    private String productName;
    private Integer productVariantId;
    private String size;
    private String color;
    private Integer stockAvailable;
    private Integer quantity;
    private Double unitPrice;
    private Double subTotal;
}
