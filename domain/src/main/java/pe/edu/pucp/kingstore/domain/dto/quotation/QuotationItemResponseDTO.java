package pe.edu.pucp.kingstore.domain.dto.quotation;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO de respuesta para ítems de cotización en el panel del comerciante.
 */
@Data
@AllArgsConstructor
public class QuotationItemResponseDTO {
    private String product;
    private String variant;
    private int quantity;
    private double price;
    private double subTotal;
}