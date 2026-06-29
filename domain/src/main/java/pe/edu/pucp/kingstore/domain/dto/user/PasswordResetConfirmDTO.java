package pe.edu.pucp.kingstore.domain.dto.user;

import lombok.Data;
import lombok.ToString;

@Data
public class PasswordResetConfirmDTO {
    @ToString.Exclude
    private String token;

    @ToString.Exclude
    private String newPassword;
}
