package pe.edu.pucp.kingstore.dto.user;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MerchantDTO extends PersonDTO {
    private UserAccountDTO userAccount;
    private String ruc;
}
