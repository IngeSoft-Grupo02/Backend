package pe.edu.pucp.kingstore.repository.product;

import pe.edu.pucp.kingstore.domain.model.product.Attribute;
import pe.edu.pucp.kingstore.domain.model.product.Discount;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import pe.edu.pucp.kingstore.domain.model.product.enums.Size;
import pe.edu.pucp.kingstore.domain.model.product.enums.VolumeType;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.StoreCategory;
import pe.edu.pucp.kingstore.domain.model.store.enums.PrimaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.SecondaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.domain.model.store.enums.TertiaryColor;

import java.util.ArrayList;
import java.util.List;

public class ProductTestDataUtil {
    //Product
    public static Product createTestProductA(Store store) {
        Product product = new Product();
        product.setStore(store);
        product.setName("ZAPATILLAS HOMBRE NEW BALANCE RUNNING AZUL 413");
        product.setImageUrls(new ArrayList<>(List.of(
                "img1.jpg",
                "img2.jpg",
                "img3.jpg")
        ));
        product.setCostPrice(55.4);
        product.setBasePrice(70.4);
        product.setDescription("test product A");

        //ProductVariant List
        ProductVariant v1 = createTestProductVariantA();
        ProductVariant v2 = createTestProductVariantB();
        ProductVariant v3 = createTestProductVariantC();

        product.setVariants(new ArrayList<>());
        product.getVariants().add(v1);
        product.getVariants().add(v2);
        product.getVariants().add(v3);

        //Attribute List
        Attribute a1 = createTestAttributeA();
        Attribute a2 = createTestAttributeB();
        Attribute a3 = createTestAttributeC();
        product.setAttributes(new ArrayList<>());
        product.getAttributes().add(a1);
        product.getAttributes().add(a2);
        product.getAttributes().add(a3);

        return product;
    }

    public static Product createTestProductB(Store store) {
        Product product = new Product();
        product.setStore(store);
        product.setName("ZAPATILLAS MUJER REEBOK TRAINING NEGRO FLEXAGON ENERGY TR 4");
        product.setImageUrls(new ArrayList<>(List.of(
                "img1.jpg",
                "img2.jpg",
                "img3.jpg")
        ));
        product.setCostPrice(45.2);
        product.setBasePrice(60.8);
        product.setDescription("test product B");

        //ProductVariant List
        ProductVariant v1 = createTestProductVariantA();
        ProductVariant v2 = createTestProductVariantB();
        ProductVariant v3 = createTestProductVariantC();

        product.setVariants(new ArrayList<>());
        product.getVariants().add(v1);
        product.getVariants().add(v2);
        product.getVariants().add(v3);

        //Attribute List
        Attribute a1 = createTestAttributeA();
        Attribute a2 = createTestAttributeB();
        Attribute a3 = createTestAttributeC();
        product.setAttributes(new ArrayList<>());
        product.getAttributes().add(a1);
        product.getAttributes().add(a2);
        product.getAttributes().add(a3);

        return product;
    }

    public static Product createTestProductC(Store store) {
        Product product = new Product();
        product.setActive(false);
        product.setStore(store);
        product.setName("ZAPATOS HOMBRE NAVIGATA RACAO");
        product.setImageUrls(new ArrayList<>(List.of(
                "img1.jpg",
                "img2.jpg",
                "img3.jpg")
        ));
        product.setCostPrice(95.2);
        product.setBasePrice(100.8);
        product.setDescription("test product C");

        //ProductVariant List
        ProductVariant v1 = createTestProductVariantA();
        ProductVariant v2 = createTestProductVariantB();
        ProductVariant v3 = createTestProductVariantC();

        product.setVariants(new ArrayList<>());
        product.getVariants().add(v1);
        product.getVariants().add(v2);
        product.getVariants().add(v3);

        //Attribute List
        Attribute a1 = createTestAttributeA();
        Attribute a2 = createTestAttributeB();
        Attribute a3 = createTestAttributeC();
        product.setAttributes(new ArrayList<>());
        product.getAttributes().add(a1);
        product.getAttributes().add(a2);
        product.getAttributes().add(a3);

        return product;
    }

    //Store
    public static Store createTestStoreA() {
        Store store = new Store();
        store.setStoreName("Ripley");
        store.setSlug("ripley");
        store.setCategory(category(1));
        store.setPrimaryColor(PrimaryColor.ONYX_BLACK);
        store.setSecondaryColor(SecondaryColor.SLATE);
        store.setTertiaryColor(TertiaryColor.RAW_GOLD);
        store.setStoreStatus(StoreStatus.ACTIVE);
        return store;
    }

    public static Store createTestStoreB() {
        Store store = new Store();
        store.setStoreName("Saga");
        store.setSlug("saga");
        store.setCategory(category(2));
        store.setPrimaryColor(PrimaryColor.ALABASTER);
        store.setSecondaryColor(SecondaryColor.SAGE);
        store.setTertiaryColor(TertiaryColor.COPPER);
        store.setStoreStatus(StoreStatus.ACTIVE);
        return store;
    }

    private static StoreCategory category(Integer id) {
        StoreCategory category = new StoreCategory();
        category.setId(id);
        return category;
    }
    //Attribute
    public static Attribute createTestAttributeA() {
        Attribute attribute = new Attribute();
        attribute.setName("nameA");
        attribute.setValue("valueA");
        return attribute;
    }
    public static Attribute createTestAttributeB() {
        Attribute attribute = new Attribute();
        attribute.setName("nameB");
        attribute.setValue("valueB");
        return attribute;
    }
    public static Attribute createTestAttributeC() {
        Attribute attribute = new Attribute();
        attribute.setName("nameC");
        attribute.setValue("valueC");
        return attribute;
    }

    //ProductVariant
    public static ProductVariant createTestProductVariantA() {
        ProductVariant variant = new ProductVariant();
        variant.setSize("M");
        variant.setColor(Color.RED);
        variant.setStock(10);
        return variant;
    }

    public static ProductVariant createTestProductVariantB() {
        ProductVariant variant = new ProductVariant();
        variant.setSize("L"); //modificado
        variant.setColor(Color.BLUE);
        variant.setStock(5);
        return variant;
    }

    public static ProductVariant createTestProductVariantC() {
        ProductVariant variant = new ProductVariant();
        variant.setSize("XL");
        variant.setColor(Color.GREEN);
        variant.setStock(40);
        return variant;
    }

    //Discounts
    public static Discount createTestDiscountA(Product product) {
        Discount discount = new Discount();
        discount.setProduct(product);
        discount.setVolumeType(VolumeType.DOZEN);
        discount.setMinQuantity(10);
        discount.setMaxQuantity(30);
        discount.setDiscountPercentage(10);
        return discount;
    }

    public static Discount createTestDiscountB(Product product) {
        Discount discount = new Discount();
        discount.setProduct(product);
        discount.setVolumeType(VolumeType.HUNDRED);
        discount.setMinQuantity(50);
        discount.setMaxQuantity(200);
        discount.setDiscountPercentage(20);
        return discount;
    }

    public static Discount createTestDiscountC(Product product) {
        Discount discount = new Discount();
        discount.setProduct(product);
        discount.setVolumeType(VolumeType.THOUSAND);
        discount.setMinQuantity(300);
        discount.setMaxQuantity(1000);
        discount.setDiscountPercentage(25);
        return discount;
    }
}


