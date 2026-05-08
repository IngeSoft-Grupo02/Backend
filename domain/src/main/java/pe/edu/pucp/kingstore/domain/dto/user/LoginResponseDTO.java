package pe.edu.pucp.kingstore.domain.dto.user;

import lombok.Data;
import pe.edu.pucp.kingstore.domain.model.user.enums.Role;

//DTO para confirma identidad SALIDA
@Data
public class LoginResponseDTO {
    private Integer id;
    private String email;
    private Role role;
}
