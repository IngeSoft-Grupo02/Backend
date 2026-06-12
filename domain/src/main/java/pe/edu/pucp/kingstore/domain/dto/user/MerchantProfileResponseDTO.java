package pe.edu.pucp.kingstore.domain.dto.user;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MerchantProfileResponseDTO {
    private Integer id;
    private String email;
    private String name;
    private String firstName;
    private String paternalSurname;
    private String maternalSurname;
    private String phone;
    private String ruc;
}