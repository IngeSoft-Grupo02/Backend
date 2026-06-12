package pe.edu.pucp.kingstore.domain.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;

import java.util.List;

@Data
@AllArgsConstructor
public class ProductResponseDTO {
    private Integer id;
    private String name;
    private String description;
    private double price;
    private double costPrice;
    private List<String> imageUrls;
    private Boolean active;
    private String status;
    private int stock;
    private List<ProductVariantResponseDTO> variants;
    private Integer storeId;

    @Data
    @AllArgsConstructor
    public static class ProductVariantResponseDTO {
        private Integer id;
        private String size;
        private Color color;
        private int stock;
    }
}