package pe.edu.pucp.kingstore.domain.dto.user;

import lombok.Data;
import pe.edu.pucp.kingstore.domain.model.user.enums.DocumentType;
import pe.edu.pucp.kingstore.domain.model.user.enums.Role;

@Data
public class CustomerProfileDTO {
    private Integer id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private DocumentType documentType;
    private String documentNumber;
    private Role role;
    private String storeSlug;
}
