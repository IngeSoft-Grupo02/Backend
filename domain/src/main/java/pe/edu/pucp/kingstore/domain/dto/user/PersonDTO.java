package pe.edu.pucp.kingstore.domain.dto.user;

import lombok.Data;
import lombok.EqualsAndHashCode;
import pe.edu.pucp.kingstore.domain.dto.BaseEntityDTO;
import pe.edu.pucp.kingstore.domain.model.user.enums.DocumentType;
import pe.edu.pucp.kingstore.domain.model.user.enums.Gender;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)

public class PersonDTO extends BaseEntityDTO {
    //se elimino ID (repetido, base entity ya lo maneja)
    private String documentNumber;
    private DocumentType documentType;
    private String firstName;
    private String paternalSurname;
    private String maternalSurname;
    private LocalDate birthDate;
    private String phone;
    private Gender gender;
}