package pe.edu.pucp.kingstore.domain.dto.bulk;

import lombok.Data;

@Data
public class BulkStoreRowDTO {
    private int    rowNumber;
    private String storeName;
    private String slug;
    private String description;
    private String categoryId;      // ID numerico de StoreCategory
    private String primaryColor;    // Enum: ONYX_BLACK, MIDNIGHT, CHARCOAL, ESPRESSO, ALABASTER, WARM_CREAM
    private String secondaryColor;  // Enum: SLATE, SAGE, TERRA, DUSTY_RED, GHOST_WHITE, SOFT_TAUPE, BLUSH_PINK, FROSTED_BLUE
    private String tertiaryColor;   // Enum: RAW_GOLD, COPPER, COBALT_BLUE, CORAL_PUNCH, EMERALD, SUNFLOWER, HOT_MAGENTA, VIOLET_POP
    private String merchantEmail;   // Email del comerciante (obligatorio)
    private String logoFileName;    // Nombre del archivo en el ZIP
}
