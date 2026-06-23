package pe.edu.pucp.kingstore.domain.dto.product;
import lombok.AllArgsConstructor;
import lombok.Data;
import pe.edu.pucp.kingstore.domain.model.product.enums.VolumeType;
/**
 * DTO público de descuento por volumen.
 * Solo expone lo que el cliente necesita para entender
 * qué descuento se aplica según la cantidad solicitada.
 * No expone datos internos del comerciante.
 */
@Data
@AllArgsConstructor



public class DiscountPublicDTO {
    private Integer id;
    private String name;
    private VolumeType volumeType;
    private int minQuantity;
    private int maxQuantity;
    private double discountPercentage;
}