package pe.edu.pucp.kingstore.domain.dto.quotation;

import lombok.Data;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;

/**
 * DTO de solicitud para responder una cotización (aprobar o rechazar).
 */
@Data
public class QuotationResponseRequestDTO {
    private QuotationStatus status;
    private String observations;
}