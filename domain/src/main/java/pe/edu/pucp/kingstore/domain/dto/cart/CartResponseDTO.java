package pe.edu.pucp.kingstore.domain.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Data;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta del carrito para el cliente.
 * Muestra el resumen completo: items, descuento aplicado,
 * subtotal y total. No expone datos internos del merchant.
 */
@Data
@AllArgsConstructor
public class CartResponseDTO {
    private Integer id;
    private List<CartItemResponseDTO> items;
    private double subTotal;
    private double discount;
    private double totalAmount;

    @Data
    @AllArgsConstructor
    public static class CartItemResponseDTO {
        private Integer id;
        private Integer productVariantId;
        private String productName;
        private String size;
        private Color color;
        private double price;
        private int quantity;
        private double subtotal;
        private double discountApplied;
        private CustomDesignResponseDTO customDesign; // null si no tiene personalización
    }
    @Data
    @AllArgsConstructor
    public static class CustomDesignResponseDTO {
        private Integer id;
        private String imageUrl;
        private String description;
        private String observations;
        private LocalDateTime sentAt;
    }
}