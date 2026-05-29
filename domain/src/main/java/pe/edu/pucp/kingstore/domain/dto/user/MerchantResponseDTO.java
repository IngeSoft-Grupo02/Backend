package pe.edu.pucp.kingstore.domain.dto.user;

import lombok.Data;

/**
 * DTO liviano para el selector de comerciantes al crear/editar tiendas.
 * Usado por GET /admin/merchants
 */
@Data
public class MerchantResponseDTO {
    private Integer id;
    private String email;
    private String firstName;
    private String paternalSurname;
    private String ruc;
}
