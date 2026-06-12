package pe.edu.pucp.kingstore.domain.dto.product;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Data;
import pe.edu.pucp.kingstore.domain.model.product.enums.VolumeType;

/**
 * DTO de solicitud para crear o actualizar un descuento desde el panel del comerciante.
 */
@Data
public class DiscountRequestDTO {
    private Integer productId;
    private VolumeType volumeType;
    @JsonAlias("minUnits")
    private Integer minQuantity;
    private Integer maxQuantity;
    @JsonAlias("value")
    private Double discountPercentage;
    private Boolean active;
    private String status;
    private String name;
    private String type;
    private String appliesTo;
    private Integer usageCount;
}