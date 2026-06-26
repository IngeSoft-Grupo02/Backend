package pe.edu.pucp.kingstore.domain.dto.quotation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta para cotizaciones en el panel del comerciante.
 * Contiene datos calculados como statusLabel y customerName
 * que no forman parte de la entidad Quotation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuotationResponseDTO {
    private Integer id;
    private String customer;
    private QuotationStatus status;
    private String statusLabel;
    private double subTotal;
    private double discount;
    private double totalAmount;
    private LocalDateTime requestedAt;
    private LocalDateTime responseAt;
    private String description;
    private String observations;
    private Integer storeId;
    private List<QuotationItemResponseDTO> items;
    private List<QuotationDesignDTO> designs;

    // Datos reales del cliente (quotation -> shoppingCart -> customer -> person/userAccount).
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String documentType;
    private String documentNumber;
}
