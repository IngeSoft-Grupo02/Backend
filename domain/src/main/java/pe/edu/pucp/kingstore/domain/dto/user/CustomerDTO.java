package pe.edu.pucp.kingstore.domain.dto.user;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomerDTO extends PersonDTO {
    private UserAccountDTO userAccount;
}