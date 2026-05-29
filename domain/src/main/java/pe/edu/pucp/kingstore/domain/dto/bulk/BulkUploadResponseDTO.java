package pe.edu.pucp.kingstore.domain.dto.bulk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Respuesta unificada de la operación de carga masiva.
 * El frontend lee este objeto para pintar métricas e incidencias.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUploadResponseDTO {

    // ── métricas ──────────────────────────────────────────────────
    private int merchantsProcessed;
    private int merchantsCreated;
    private int storesProcessed;
    private int storesCreated;
    private int logosUploaded;
    private int errorCount;

    // ── incidencias ───────────────────────────────────────────────
    @Builder.Default
    private List<BulkIncidenceDTO> incidences = new ArrayList<>();

    // ── helpers ───────────────────────────────────────────────────
    public void addIncidence(BulkIncidenceDTO inc) {
        this.incidences.add(inc);
        if (inc.getType() == BulkIncidenceDTO.IncidenceType.ERROR) {
            this.errorCount++;
        }
    }
}
