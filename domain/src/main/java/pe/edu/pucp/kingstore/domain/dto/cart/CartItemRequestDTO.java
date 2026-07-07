package pe.edu.pucp.kingstore.domain.dto.cart;

import lombok.Data;

/**
 * DTO de entrada para agregar o actualizar un item en el carrito.
 * El cliente envía qué variante quiere y en qué cantidad.
 */
@Data
public class CartItemRequestDTO {
    private Integer productVariantId;
    private Integer quantity;
    private String selectedProductImageUrl;
    /**
     * Cuando el cliente agrega la misma variante con comentario o archivos de
     * diseño, se debe crear una línea independiente para no mezclarla con el
     * producto sin diseño.
     */
    private Boolean separateItem;
}
