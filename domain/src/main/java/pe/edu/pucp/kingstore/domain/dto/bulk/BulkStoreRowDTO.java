package pe.edu.pucp.kingstore.domain.dto.bulk;

import lombok.Data;

@Data
public class BulkStoreRowDTO {
    private int    rowNumber;
    private String storeName;
    private String slug;
    private String description;
    private String categoryId;      // ID numérico de StoreCategory
    private String primaryColor;    // Enum: ONYX_BLACK, DEEP_ZINC, MIDNIGHT, CHARCOAL, ESPRESSO
    private String secondaryColor;  // Enum: OLIVE_DRAB, SAGE, SLATE, TERRA, DUSTY_RED
    private String tertiaryColor;   // Enum: RICH_CAMEL, RAW_GOLD, SILVER_MIST, COPPER, STONE
    private String merchantEmail;   // Email del comerciante (obligatorio)
    private String logoFileName;    // Nombre del archivo en el ZIP
}
