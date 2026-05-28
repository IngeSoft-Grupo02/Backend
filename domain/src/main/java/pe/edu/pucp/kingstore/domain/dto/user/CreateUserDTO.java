package pe.edu.pucp.kingstore.domain.dto.user;

import lombok.Data;
import pe.edu.pucp.kingstore.domain.model.user.enums.DocumentType;
import pe.edu.pucp.kingstore.domain.model.user.enums.Gender;
import pe.edu.pucp.kingstore.domain.model.user.enums.Role;

import java.time.LocalDate;

@Data
public class CreateUserDTO {
    // Datos de UserAccount
    private String email;
    private String password;

    // Datos de Person
    private String documentNumber;
    private DocumentType documentType;
    private String firstName;
    private String paternalSurname;
    private String maternalSurname;
    private LocalDate birthDate;
    private String phone;
    private Gender gender;

    private Integer merchantId;

    // Rol a asignar
    private Role role;

    // Solo obligatorio si role == MERCHANT
    private String ruc;

    // Solo obligatorio si role == CUSTOMER
    private Integer storeId;
}