package pe.edu.pucp.kingstore.domain.dto.store;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * DTO de respuesta para tiendas en el panel del comerciante.
 */
@Data
@AllArgsConstructor
public class StoreResponseDTO {
    private Integer id;
    private String name;
    private String slug;
    private String description;
    private String logoUrl;
    private String status;
    private String primaryColor;
    private String secondaryColor;
    private String tertiaryColor;
    private Integer categoryId;
    private String categoryName;
    private long pendingQuotes;
}