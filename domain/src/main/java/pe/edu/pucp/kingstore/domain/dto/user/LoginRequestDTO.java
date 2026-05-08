package pe.edu.pucp.kingstore.domain.dto.user;
//DTO SOLO para recibir credenciale

import lombok.Data;

@Data
public class LoginRequestDTO {
    private String email;
    private String password;
}
