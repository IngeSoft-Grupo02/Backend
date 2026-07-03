package pe.edu.pucp.kingstore.domain.dto.product;

import lombok.Data;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;

import java.util.List;

@Data
public class ProductRequestDTO {
    private String name;
    private String description;
    private Double price;
    private Double costPrice;
    private List<String> imageUrls;
    private Boolean customizable;
    private List<ProductVariantRequestDTO> variants;
    private Boolean active;
    private String status;

    @Data
    public static class ProductVariantRequestDTO {
        private String size;
        private Color color;
        private Integer stock;
    }
}
