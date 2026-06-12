package pe.edu.pucp.kingstore.domain.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import pe.edu.pucp.kingstore.domain.model.product.enums.VolumeType;

/**
 * DTO de respuesta para descuentos en el panel del comerciante.
 */
@Data
@AllArgsConstructor
public class DiscountResponseDTO {
    private Integer id;
    private Integer productId;
    private VolumeType volumeType;
    private int minQuantity;
    private int maxQuantity;
    private double discountPercentage;
    private Boolean active;
    private String status;
    private Integer storeId;
    private String productName;
    private String name;
    private String type;
    private double value;
    private int minUnits;
    private int usageCount;
    private String appliesTo;
}