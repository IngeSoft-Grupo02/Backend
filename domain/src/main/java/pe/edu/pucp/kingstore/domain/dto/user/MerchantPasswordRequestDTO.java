package pe.edu.pucp.kingstore.domain.dto.user;

import lombok.Data;

@Data
public class MerchantPasswordRequestDTO {
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;
}