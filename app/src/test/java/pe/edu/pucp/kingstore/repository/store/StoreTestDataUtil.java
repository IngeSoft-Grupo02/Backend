package pe.edu.pucp.kingstore.repository.store;

import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.StoreCategory;
import pe.edu.pucp.kingstore.domain.model.store.enums.PrimaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.SecondaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.domain.model.store.enums.TertiaryColor;

public class StoreTestDataUtil {
    //Store
    public static Store createTestStoreA() {
        Store store = new Store();
        store.setStoreName("Ripley");
        store.setSlug("ripley");
        store.setDescription("description A");
        store.setLogoUrl("logo A");
        store.setCategory(category(1));
        store.setPrimaryColor(PrimaryColor.ONYX_BLACK);
        store.setSecondaryColor(SecondaryColor.OLIVE_DRAB);
        store.setTertiaryColor(TertiaryColor.RICH_CAMEL);
        store.setStoreStatus(StoreStatus.ACTIVE);
        return store;
    }

    public static Store createTestStoreB() {
        Store store = new Store();
        store.setStoreName("Saga");
        store.setSlug("saga");
        store.setDescription("description B");
        store.setLogoUrl("logo B");
        store.setCategory(category(2));
        store.setPrimaryColor(PrimaryColor.DEEP_ZINC);
        store.setSecondaryColor(SecondaryColor.SAGE);
        store.setTertiaryColor(TertiaryColor.SILVER_MIST);
        store.setActive(false);
        store.setStoreStatus(StoreStatus.ACTIVE);
        return store;
    }

    public static Store createTestStoreC() {
        Store store = new Store();
        store.setStoreName("Bata");
        store.setSlug("bata");
        store.setDescription("description c");
        store.setLogoUrl("logo C");
        store.setCategory(category(3));
        store.setPrimaryColor(PrimaryColor.MIDNIGHT);
        store.setSecondaryColor(SecondaryColor.SLATE);
        store.setTertiaryColor(TertiaryColor.STONE);
        store.setStoreStatus(StoreStatus.ACTIVE);
        return store;
    }

    private static StoreCategory category(Integer id) {
        StoreCategory category = new StoreCategory();
        category.setId(id);
        return category;
    }

}
