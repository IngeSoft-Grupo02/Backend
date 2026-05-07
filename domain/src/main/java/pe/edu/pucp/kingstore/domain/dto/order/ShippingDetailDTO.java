package pe.edu.pucp.kingstore.domain.dto.order;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.cglib.core.Local;
import pe.edu.pucp.kingstore.domain.dto.BaseEntityDTO;
import pe.edu.pucp.kingstore.domain.model.order.enums.District;

import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = true)
public class ShippingDetailDTO extends BaseEntityDTO {
    private String description;
    private LocalDate estimatedDeliveryDate;
    private LocalDate actualDeliveryDate;
    private String address;
    private District district;
}
