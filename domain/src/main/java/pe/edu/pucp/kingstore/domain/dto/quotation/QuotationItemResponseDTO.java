package pe.edu.pucp.kingstore.domain.dto.quotation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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
    private Integer id;
    private Integer productId;
    private String productName;
    private String productImageUrl;
    private Integer productVariantId;
    private String size;
    private String color;
    private Integer stockAvailable;
    private Integer physicalStock;
    private Integer reservedStock;
    private Integer stockShortage;
    private int quantity;
    private double unitPrice;
    private double subTotal;
    private double baseUnitPrice;
    private double baseSubtotal;
    private double discountAmount;
    private double designFeeAmount;
    private double designFeePercentage;
    private double lineTotal;
    private String discountRuleLabel;
    private boolean hasDesignFee;

    private String customerDescription;
    private List<QuotationDesignDTO> designs;

    // Legacy (no eliminar: el frontend actual los consume).
    private String product;
    private String variant;
    private double price;
}
