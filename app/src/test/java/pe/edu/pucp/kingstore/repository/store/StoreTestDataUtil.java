package pe.edu.pucp.kingstore.repository.store;

import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.enums.ColorPalette;
import pe.edu.pucp.kingstore.domain.model.store.enums.CustomerGender;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreCategory;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;

import java.util.ArrayList;

public class StoreTestDataUtil {
    //Store
    public static Store createTestStoreA() {
        Store store = new Store();
        store.setStoreName("Ripley");
        store.setSlug("ripley");
        store.setColorPalette(ColorPalette.CORESTREET);
        store.setDescription("description A");
        store.setLogoUrl("logo A");
        store.setPrimaryColor("blue");
        store.setSecondaryColor("cyan");

        //Store Category
        store.setCategories(new ArrayList<>());
        store.getCategories().add(StoreCategory.URBAN);
        store.getCategories().add(StoreCategory.SPORTSWEAR);

        //Store Gender
        store.setGenders(new ArrayList<>());
        store.getGenders().add(CustomerGender.MEN);
        store.getGenders().add(CustomerGender.WOMEN);

        store.setStoreStatus(StoreStatus.ACTIVE);
        return store;
    }

    public static Store createTestStoreB() {
        Store store = new Store();
        store.setStoreName("Saga");
        store.setSlug("saga");
        store.setColorPalette(ColorPalette.ATELIERMONO);
        store.setDescription("description B");
        store.setLogoUrl("logo B");
        store.setPrimaryColor("red");
        store.setSecondaryColor("orange");
        store.setActive(false);

        //Store Category
        store.setCategories(new ArrayList<>());
        store.getCategories().add(StoreCategory.SPORTSWEAR);
        store.getCategories().add(StoreCategory.CASUAL);

        //Store Gender
        store.setGenders(new ArrayList<>());
        store.getGenders().add(CustomerGender.WOMEN);
        store.getGenders().add(CustomerGender.KIDS);

        store.setStoreStatus(StoreStatus.ACTIVE);
        return store;
    }

    public static Store createTestStoreC() {
        Store store = new Store();
        store.setStoreName("Bata");
        store.setSlug("bata");
        store.setColorPalette(ColorPalette.LUXECAPSULE);
        store.setDescription("description c");
        store.setLogoUrl("logo C");
        store.setPrimaryColor("green");
        store.setSecondaryColor("yellow");

        //Store Category
        store.setCategories(new ArrayList<>());
        store.getCategories().add(StoreCategory.URBAN);
        store.getCategories().add(StoreCategory.CASUAL);

        //Store Gender
        store.setGenders(new ArrayList<>());
        store.getGenders().add(CustomerGender.MEN);
        store.getGenders().add(CustomerGender.KIDS);

        store.setStoreStatus(StoreStatus.ACTIVE);
        return store;
    }

}
