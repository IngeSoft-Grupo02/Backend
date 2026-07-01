package pe.edu.pucp.kingstore.domain.dto.user;

import lombok.Data;

import java.time.LocalDate;

/**
 * DTO de respuesta para listado de usuarios con rol, nombre y tienda.
 * Usado por GET /admin/users
 */
@Data
public class UserResponseDTO {
    private Integer id;
    private String email;
    private Boolean active;
    private String role;           // SYSTEM_ADMIN | MERCHANT | CUSTOMER
    private String firstName;
    private String paternalSurname;
    private String maternalSurname;
    private String documentNumber;
    private String documentType;
    private LocalDate birthDate;
    private String phone;
    private String gender;
    private String ruc;            // solo MERCHANT
    private String storeName;      // tienda asociada (MERCHANT o CUSTOMER)
    private Integer storeId;       // id de tienda asociada
}
