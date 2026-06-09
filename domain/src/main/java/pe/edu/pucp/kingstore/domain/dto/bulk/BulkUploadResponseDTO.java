package pe.edu.pucp.kingstore.domain.dto.bulk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta unificada de la operaciÃ³n de carga masiva.
 * El frontend lee este objeto para pintar mÃ©tricas e incidencias.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadResponseDTO {

    // â”€â”€ mÃ©tricas â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private int merchantsProcessed;
    private int merchantsCreated;
    private int storesProcessed;
    private int storesCreated;
    private int logosUploaded;
    private int errorCount;

    // â”€â”€ incidencias â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Builder.Default
    private List<BulkIncidenceDTO> incidences = new ArrayList<>();

    // â”€â”€ helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public void addIncidence(BulkIncidenceDTO inc) {
        this.incidences.add(inc);
        if (inc.getType() == BulkIncidenceDTO.IncidenceType.ERROR) {
            this.errorCount++;
        }
    }
}
