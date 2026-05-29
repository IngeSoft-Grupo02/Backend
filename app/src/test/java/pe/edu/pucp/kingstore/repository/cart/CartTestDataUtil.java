package pe.edu.pucp.kingstore.repository.cart;

import pe.edu.pucp.kingstore.domain.model.cart.CartItem;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import pe.edu.pucp.kingstore.domain.model.product.enums.Material;
import pe.edu.pucp.kingstore.domain.model.product.enums.Size;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.StoreCategory;
import pe.edu.pucp.kingstore.domain.model.store.enums.PrimaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.SecondaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.domain.model.store.enums.TertiaryColor;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
import pe.edu.pucp.kingstore.domain.model.user.enums.DocumentType;
import pe.edu.pucp.kingstore.domain.model.user.enums.Gender;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class CartTestDataUtil {
    //Shopping Cart
    public static ShoppingCart createShoppingCartA(Customer customer, Product product) {
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setCustomer(customer);
        shoppingCart.setDiscount(0.5);
        shoppingCart.setSubTotal(20.0);
        shoppingCart.setTotalAmount(40.0);

        //CartItems

        CartItem i1 = createCartItemA(product.getVariants().getFirst());
        CartItem i2 = createCartItemB(product.getVariants().get(1));
        CartItem i3 = createCartItemC(product.getVariants().get(2));

        shoppingCart.setItems(new ArrayList<>());
        shoppingCart.getItems().add(i1);
        shoppingCart.getItems().add(i2);
        shoppingCart.getItems().add(i3);

        return shoppingCart;
    }

    public static ShoppingCart createShoppingCartB(Customer customer, Product product) {
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setCustomer(customer);
        shoppingCart.setDiscount(0.4);
        shoppingCart.setSubTotal(10.0);
        shoppingCart.setTotalAmount(20.0);

        //CartItems

        CartItem i1 = createCartItemA(product.getVariants().getFirst());
        CartItem i2 = createCartItemB(product.getVariants().get(1));
        CartItem i3 = createCartItemC(product.getVariants().get(2));

        shoppingCart.setItems(new ArrayList<>());
        shoppingCart.getItems().add(i1);
        shoppingCart.getItems().add(i2);
        shoppingCart.getItems().add(i3);

        return shoppingCart;
    }

    public static ShoppingCart createShoppingCartC(Customer customer, Product product) {
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setCustomer(customer);
        shoppingCart.setDiscount(0.2);
        shoppingCart.setSubTotal(30.0);
        shoppingCart.setTotalAmount(100.0);

        //CartItems

        CartItem i1 = createCartItemA(product.getVariants().getFirst());
        CartItem i2 = createCartItemB(product.getVariants().get(1));
        CartItem i3 = createCartItemC(product.getVariants().get(2));

        shoppingCart.setItems(new ArrayList<>());
        shoppingCart.getItems().add(i1);
        shoppingCart.getItems().add(i2);
        shoppingCart.getItems().add(i3);

        return shoppingCart;
    }

    //Cart Item
    private static CartItem createCartItemA(ProductVariant variant) {
        CartItem cartItem = new CartItem();
        cartItem.setPrice(32.30);
        cartItem.setQuantity(1);
        cartItem.setProductVariant(variant);
        cartItem.setSubtotal(32.30);
        return cartItem;
    }
    private static CartItem createCartItemB(ProductVariant variant) {
        CartItem cartItem = new CartItem();
        cartItem.setPrice(22.30);
        cartItem.setQuantity(2);
        cartItem.setProductVariant(variant);
        cartItem.setSubtotal(44.60);
        return cartItem;
    }
    private static CartItem createCartItemC(ProductVariant variant) {
        CartItem cartItem = new CartItem();
        cartItem.setPrice(12.30);
        cartItem.setQuantity(3);
        cartItem.setProductVariant(variant);
        cartItem.setSubtotal(36.90);
        return cartItem;
    }

    //Customer
    public static Customer createCustomerA(UserAccount userAccount) {
        Customer customer = new Customer();
        customer.setUserAccount(userAccount);
        customer.setBirthDate(LocalDate.of(2003,9,17));
        customer.setFirstName("Juan");
        customer.setPaternalSurname("Perez");
        customer.setMaternalSurname("Perez");
        customer.setDocumentType(DocumentType.DNI);
        customer.setDocumentNumber("12345678");
        customer.setGender(Gender.MALE);
        return customer;
    }

    public static Customer createCustomerB(UserAccount userAccount) {
        Customer customer = new Customer();
        customer.setUserAccount(userAccount);
        customer.setBirthDate(LocalDate.of(2002,11,16));
        customer.setFirstName("Marcelo");
        customer.setPaternalSurname("Lopez");
        customer.setMaternalSurname("Mamani");
        customer.setDocumentType(DocumentType.DNI);
        customer.setDocumentNumber("87654321");
        customer.setGender(Gender.MALE);
        return customer;
    }

    //User Account
    public static UserAccount createUserAccountA() {
        UserAccount userAccount = new UserAccount();
        userAccount.setEmail("userA@example");
        userAccount.setPassword("password");
        return userAccount;
    }
    public static UserAccount createUserAccountB() {
        UserAccount userAccount = new UserAccount();
        userAccount.setEmail("userB@example");
        userAccount.setPassword("password");
        return userAccount;
    }


    //Product
    public static Product createTestProduct(Store store) {
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
        product.setMaterial(Material.SYNTHETIC);

        //ProductVariant List
        ProductVariant v1 = createTestProductVariantA();
        ProductVariant v2 = createTestProductVariantB();
        ProductVariant v3 = createTestProductVariantC();

        product.setVariants(new ArrayList<>());
        product.getVariants().add(v1);
        product.getVariants().add(v2);
        product.getVariants().add(v3);

        return product;
    }

    //Store
    public static Store createTestStore() {
        Store store = new Store();
        store.setStoreName("Ripley");
        store.setSlug("ripley");
        store.setCategory(category(1));
        store.setPrimaryColor(PrimaryColor.ONYX_BLACK);
        store.setSecondaryColor(SecondaryColor.OLIVE_DRAB);
        store.setTertiaryColor(TertiaryColor.RICH_CAMEL);
        store.setStoreStatus(StoreStatus.ACTIVE);
        return store;
    }

    private static StoreCategory category(Integer id) {
        StoreCategory category = new StoreCategory();
        category.setId(id);
        return category;
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
        variant.setSize("L");
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
}
