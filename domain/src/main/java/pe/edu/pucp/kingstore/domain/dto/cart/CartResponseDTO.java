package pe.edu.pucp.kingstore.domain.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de respuesta del carrito para el cliente.
 * Muestra el resumen completo: items, descuento aplicado,
 * subtotal y total. No expone datos internos del merchant.
 */
@Data
@NoArgsConstructor
public class CartResponseDTO {
    private Integer id;
    private List<CartItemResponseDTO> items;
    private double subTotal;
    private double discount;
    private double totalAmount;
    private double productSubtotal;
    private double discountTotal;
    private double designFeeTotal;

    public CartResponseDTO(Integer id, List<CartItemResponseDTO> items,
                           double subTotal, double discount, double totalAmount) {
        this(id, items, subTotal, discount, totalAmount, 0, discount, 0);
    }

    public CartResponseDTO(Integer id, List<CartItemResponseDTO> items,
                           double subTotal, double discount, double totalAmount,
                           double productSubtotal, double discountTotal,
                           double designFeeTotal) {
        this.id = id;
        this.items = items;
        this.subTotal = subTotal;
        this.discount = discount;
        this.totalAmount = totalAmount;
        this.productSubtotal = productSubtotal;
        this.discountTotal = discountTotal;
        this.designFeeTotal = designFeeTotal;
    }

    @Data
    @NoArgsConstructor
    public static class CartItemResponseDTO {
        private Integer id;
        private Integer productId;
        private Integer productVariantId;
        private String productName;
        private String productImageUrl;
        private String size;
        private Color color;
        private double price;
        private int quantity;
        private double subtotal;
        private double discountApplied;
        private CustomDesignResponseDTO customDesign; // null si no tiene personalización
        private double baseUnitPrice;
        private double baseSubtotal;
        private double discountAmount;
        private double designFeeAmount;
        private double lineTotal;
        private String discountRuleLabel;
        private boolean hasDesignFee;

        public CartItemResponseDTO(Integer id, Integer productId,
                                   Integer productVariantId, String productName,
                                   String size, Color color, double price,
                                   int quantity, double subtotal,
                                   double discountApplied,
                                   CustomDesignResponseDTO customDesign) {
            this(id, productId, productVariantId, productName, size, color,
                    price, quantity, subtotal, discountApplied, customDesign,
                    null, price, price * quantity, 0, 0, subtotal, null, false);
        }

        public CartItemResponseDTO(Integer id, Integer productId,
                                   Integer productVariantId, String productName,
                                   String size, Color color, double price,
                                   int quantity, double subtotal,
                                   double discountApplied,
                                   CustomDesignResponseDTO customDesign,
                                   String productImageUrl,
                                   double baseUnitPrice, double baseSubtotal,
                                   double discountAmount, double designFeeAmount,
                                   double lineTotal, String discountRuleLabel,
                                   boolean hasDesignFee) {
            this.id = id;
            this.productId = productId;
            this.productVariantId = productVariantId;
            this.productName = productName;
            this.productImageUrl = productImageUrl;
            this.size = size;
            this.color = color;
            this.price = price;
            this.quantity = quantity;
            this.subtotal = subtotal;
            this.discountApplied = discountApplied;
            this.customDesign = customDesign;
            this.baseUnitPrice = baseUnitPrice;
            this.baseSubtotal = baseSubtotal;
            this.discountAmount = discountAmount;
            this.designFeeAmount = designFeeAmount;
            this.lineTotal = lineTotal;
            this.discountRuleLabel = discountRuleLabel;
            this.hasDesignFee = hasDesignFee;
        }
    }
    @Data
    @AllArgsConstructor
    public static class CustomDesignResponseDTO {
        private Integer id;
        private String imageUrl;
        private String description;
        private String observations;
        private LocalDateTime sentAt;
        private Double overlayX;
        private Double overlayY;
        private Double overlayWidth;
        private Double overlayHeight;
    }
}
