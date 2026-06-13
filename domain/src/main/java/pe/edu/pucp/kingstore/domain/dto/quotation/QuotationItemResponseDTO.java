package pe.edu.pucp.kingstore.domain.dto.quotation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para ítems de cotización en el panel del comerciante.
 *
 * Campos nuevos (datos reales): productId, productName, productVariantId, size,
 * color, stockAvailable, quantity, unitPrice, subTotal.
 *
 * Campos legacy mantenidos por compatibilidad con el frontend actual:
 * product (= productName), variant (= "size / color"), price (= unitPrice).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuotationItemResponseDTO {
    private Integer productId;
    private String productName;
    private Integer productVariantId;
    private String size;
    private String color;
    private Integer stockAvailable;
    private int quantity;
    private double unitPrice;
    private double subTotal;

    // Legacy (no eliminar: el frontend actual los consume).
    private String product;
    private String variant;
    private double price;
}
