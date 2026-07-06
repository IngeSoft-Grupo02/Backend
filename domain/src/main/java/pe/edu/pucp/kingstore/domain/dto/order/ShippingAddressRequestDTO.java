package pe.edu.pucp.kingstore.domain.dto.order;

import lombok.Data;

@Data
public class ShippingAddressRequestDTO {
    private String address;
    private String district;
    private String reference;
    private String recipientName;
    private String phone;
}
