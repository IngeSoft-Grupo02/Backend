package pe.edu.pucp.kingstore.domain.dto.user;

import lombok.Data;

@Data
public class MerchantProfileRequestDTO {
    private String email;
    private String firstName;
    private String paternalSurname;
    private String maternalSurname;
    private String phone;
}