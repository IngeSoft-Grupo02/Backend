package pe.edu.pucp.kingstore.domain.dto.product;

import lombok.Data;
import lombok.EqualsAndHashCode;
import pe.edu.pucp.kingstore.domain.dto.BaseEntityDTO;
import pe.edu.pucp.kingstore.domain.dto.store.StoreDTO;
import pe.edu.pucp.kingstore.domain.model.product.enums.Material;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductDTO extends BaseEntityDTO {
    private String storeSlug; // identificador
    private String name;
    private List<String> imageUrls;
    private double costPrice;
    private double basePrice;
    private Material material;
    private List <ProductVariantDTO> variants;
    private String description;
}
