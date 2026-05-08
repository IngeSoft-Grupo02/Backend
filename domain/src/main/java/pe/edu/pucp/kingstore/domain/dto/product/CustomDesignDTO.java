package pe.edu.pucp.kingstore.domain.dto.product;

import lombok.Data;
import lombok.EqualsAndHashCode;
import pe.edu.pucp.kingstore.domain.dto.BaseEntityDTO;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class CustomDesignDTO extends BaseEntityDTO {
    private String imageUrl;
    private String description;
    private String observations;
    private LocalDateTime sentAt;
    private LocalDateTime reviewedAt;
    private Integer productId;

}
