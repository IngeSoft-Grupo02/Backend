package pe.edu.pucp.kingstore.domain.model.store.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StoreCategoryEnumTest {

    @Test
    void exposesAllStoreCategoryValues() {
        assertThat(StoreCategory.values())
                .containsExactly(StoreCategory.FORMAL, StoreCategory.CASUAL, StoreCategory.SPORTSWEAR, StoreCategory.URBAN);
        assertThat(StoreCategory.valueOf("URBAN")).isEqualTo(StoreCategory.URBAN);
    }
}
