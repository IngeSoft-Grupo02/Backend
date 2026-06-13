package pe.edu.pucp.kingstore.domain.dto.user;

import lombok.Data;
import pe.edu.pucp.kingstore.domain.model.user.enums.DocumentType;
import pe.edu.pucp.kingstore.domain.model.user.enums.Gender;

import java.time.LocalDate;

/**
 * DTO para el registro público de un cliente en una tienda específica.
 */
@Data
public class RegisterCustomerDTO {
    private String email;
    private String password;
    private String firstName;
    private String paternalSurname;
    private String maternalSurname;
    private DocumentType documentType;
    private String documentNumber;
    private String phone;
    private LocalDate birthDate;
    private Gender gender;
}