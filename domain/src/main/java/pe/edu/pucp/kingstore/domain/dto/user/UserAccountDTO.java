package pe.edu.pucp.kingstore.domain.dto.user;

import lombok.Data;
import lombok.EqualsAndHashCode;
import pe.edu.pucp.kingstore.domain.dto.BaseEntityDTO;

@Data
@EqualsAndHashCode(callSuper = true)
public class UserAccountDTO extends BaseEntityDTO {
    private String email;
    private String password;
}
