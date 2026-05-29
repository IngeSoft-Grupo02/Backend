package pe.edu.pucp.kingstore.domain.dto.bulk;

import lombok.Data;

/**
 * Representa una fila del CSV de carga masiva de tiendas.
 *
 * Columnas esperadas:
 *   storeName, slug, colorPalette (CORESTREET|ATELIERMONO|UTILITYDROP|LUXECAPSULE),
 *   description (opcional),
 *   merchantEmail (opcional – vincula la tienda al comerciante con ese email si ya existe en BD)
 *   logoFileName (opcional – nombre del archivo dentro del ZIP de logos, ej: mi_tienda.png)
 */
@Data
public class BulkStoreRowDTO {
    private int rowNumber;
    private String storeName;
    private String slug;
    private String colorPalette;
    private String description;
    private String merchantEmail;   // opcional
    private String logoFileName;    // opcional – se resuelve contra el ZIP
}
