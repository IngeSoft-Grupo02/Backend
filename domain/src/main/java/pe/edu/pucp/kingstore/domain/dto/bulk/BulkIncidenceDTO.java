package pe.edu.pucp.kingstore.domain.dto.bulk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkIncidenceDTO {

    public enum IncidenceType { ERROR, WARNING }
    public enum IncidenceBlock { MERCHANTS, STORES, IMAGES }

    private IncidenceBlock block;   // MERCHANTS | STORES | IMAGES
    private int row;                // nro de fila en el CSV original
    private String code;            // ej: VAL_EMAIL, DUPLICATE, REF_NOT_FOUND
    private IncidenceType type;
    private String detail;          // mensaje legible para el admin
    private String origin;          // nombre del archivo fuente
}
