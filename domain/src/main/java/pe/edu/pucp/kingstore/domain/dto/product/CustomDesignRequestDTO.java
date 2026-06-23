package pe.edu.pucp.kingstore.domain.dto.product;

import lombok.Data;

/**
 * DTO de entrada para registrar una personalización en un item del carrito.
 * El cliente puede subir una imagen y/o escribir una descripción.
 */
@Data
public class CustomDesignRequestDTO {
    private String imageUrl;
    private String description;
}