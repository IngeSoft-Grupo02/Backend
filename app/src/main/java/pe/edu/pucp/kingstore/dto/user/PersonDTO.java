package pe.edu.pucp.kingstore.dto.user;

import lombok.Data;
import pe.edu.pucp.kingstore.domain.model.user.enums.DocumentType;
import pe.edu.pucp.kingstore.domain.model.user.enums.Gender;
import java.time.LocalDate;

@Data
public class PersonDTO {
    private Integer id;
    private String documentNumber;
    private DocumentType documentType;
    private String firstName;
    private String paternalSurname;
    private String maternalSurname;
    private LocalDate birthDate;
    private String phone;
    private Gender gender;
}