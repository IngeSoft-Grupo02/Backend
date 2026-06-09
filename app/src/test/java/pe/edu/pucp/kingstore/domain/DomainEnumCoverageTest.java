package pe.edu.pucp.kingstore.domain;

import org.junit.jupiter.api.Test;
import pe.edu.pucp.kingstore.domain.model.product.enums.Size;
import pe.edu.pucp.kingstore.domain.model.store.enums.PrimaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.SecondaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreCategory;
import pe.edu.pucp.kingstore.domain.model.store.enums.TertiaryColor;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEnumCoverageTest {

    @Test
    void exposesStorePaletteAndProductSizeEnums() {
        assertThat(Size.values()).containsExactly(Size.XS, Size.S, Size.M, Size.L, Size.XL);
        assertThat(StoreCategory.values()).contains(StoreCategory.FORMAL, StoreCategory.CASUAL,
                StoreCategory.SPORTSWEAR, StoreCategory.URBAN);
        assertThat(PrimaryColor.valueOf("ONYX_BLACK")).isEqualTo(PrimaryColor.ONYX_BLACK);
        assertThat(SecondaryColor.valueOf("SLATE")).isEqualTo(SecondaryColor.SLATE);
        assertThat(TertiaryColor.valueOf("RAW_GOLD")).isEqualTo(TertiaryColor.RAW_GOLD);
    }
}
