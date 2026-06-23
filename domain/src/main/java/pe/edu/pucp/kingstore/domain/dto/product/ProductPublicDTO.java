package pe.edu.pucp.kingstore.domain.dto.product;

import lombok.AllArgsConstructor;
import lombok.Data;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;

import java.util.List;

/**
 * DTO público de producto para el catálogo del cliente.
 * Expone únicamente los campos que el cliente necesita ver:
 * imágenes, descripción, precio base, variantes disponibles
 * y reglas de descuento por volumen activas.
 *
 * NO expone: costPrice, active, storeId ni campos operacionales del merchant.
 */
@Data
@AllArgsConstructor
public class ProductPublicDTO {
    private Integer id;
    private String name;
    private String description;
    private double basePrice;
    private List<String> imageUrls;
    private List<ProductVariantPublicDTO> variants;
    private List<DiscountPublicDTO> discounts;

    @Data
    @AllArgsConstructor
    public static class ProductVariantPublicDTO {
        private Integer id;
        private String size;
        private Color color;
        private int stock;
    }
}