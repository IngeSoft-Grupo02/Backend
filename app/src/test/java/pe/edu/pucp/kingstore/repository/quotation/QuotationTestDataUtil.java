package pe.edu.pucp.kingstore.repository.quotation;

import pe.edu.pucp.kingstore.domain.model.cart.CartItem;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import pe.edu.pucp.kingstore.domain.model.product.enums.Material;
import pe.edu.pucp.kingstore.domain.model.product.enums.Size;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.QuotationItem;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.enums.ColorPalette;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
import pe.edu.pucp.kingstore.domain.model.user.enums.DocumentType;
import pe.edu.pucp.kingstore.domain.model.user.enums.Gender;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class QuotationTestDataUtil {

    //Quotation
    public static Quotation createQuotationA(ShoppingCart shoppingCart, Product product) {
        Quotation quotation = new Quotation();
        quotation.setShoppingCart(shoppingCart);
        quotation.setSubTotal(90.5);
        quotation.setDiscount(0.3);
        quotation.setTotalAmount(30.2);
        quotation.setStatus(QuotationStatus.PENDING);
        quotation.setResponseAt(LocalDateTime.now());
        quotation.setDescription("descriptionA");
        quotation.setObservations("observationsA");

        //QuotationItem
        QuotationItem i1 = createQuotationItemA(product);
        QuotationItem i2 = createQuotationItemB(product);
        QuotationItem i3 = createQuotationItemC(product);

        quotation.setItems(new ArrayList<>());
        quotation.getItems().add(i1);
        quotation.getItems().add(i2);
        quotation.getItems().add(i3);
        return quotation;
    }

    public static Quotation createQuotationB(ShoppingCart shoppingCart, Product product) {
        Quotation quotation = new Quotation();
        quotation.setShoppingCart(shoppingCart);
        quotation.setSubTotal(60.1);
        quotation.setDiscount(0.2);
        quotation.setTotalAmount(10.2);
        quotation.setStatus(QuotationStatus.PENDING);
        quotation.setResponseAt(LocalDateTime.now());
        quotation.setDescription("descriptionB");
        quotation.setObservations("observationsB");

        //QuotationItem
        QuotationItem i1 = createQuotationItemA(product);
        QuotationItem i2 = createQuotationItemB(product);
        QuotationItem i3 = createQuotationItemC(product);

        quotation.setItems(new ArrayList<>());
        quotation.getItems().add(i1);
        quotation.getItems().add(i2);
        quotation.getItems().add(i3);
        return quotation;
    }

    public static Quotation createQuotationC(ShoppingCart shoppingCart, Product product) {
        Quotation quotation = new Quotation();
        quotation.setShoppingCart(shoppingCart);
        quotation.setSubTotal(80.5);
        quotation.setDiscount(0.6);
        quotation.setTotalAmount(20.2);
        quotation.setStatus(QuotationStatus.REJECTED);
        quotation.setResponseAt(LocalDateTime.now());
        quotation.setDescription("descriptionC");
        quotation.setObservations("observationsC");

        //QuotationItem
        QuotationItem i1 = createQuotationItemA(product);
        QuotationItem i2 = createQuotationItemB(product);
        QuotationItem i3 = createQuotationItemC(product);

        quotation.setItems(new ArrayList<>());
        quotation.getItems().add(i1);
        quotation.getItems().add(i2);
        quotation.getItems().add(i3);
        return quotation;
    }

    //QuotationItem
    private static QuotationItem createQuotationItemA(Product product) {
        QuotationItem quotationItem = new QuotationItem();
        quotationItem.setProductVariant(product.getVariants().getFirst());
        quotationItem.setPrice(25.4);
        quotationItem.setQuantity(1);
        quotationItem.setSubTotal(25.4);
        return quotationItem;
    }

    private static QuotationItem createQuotationItemB(Product product) {
        QuotationItem quotationItem = new QuotationItem();
        quotationItem.setProductVariant(product.getVariants().get(1));
        quotationItem.setPrice(15.4);
        quotationItem.setQuantity(2);
        quotationItem.setSubTotal(30.8);
        return quotationItem;
    }

    private static QuotationItem createQuotationItemC(Product product) {
        QuotationItem quotationItem = new QuotationItem();
        quotationItem.setProductVariant(product.getVariants().get(2));
        quotationItem.setPrice(35.4);
        quotationItem.setQuantity(3);
        quotationItem.setSubTotal(106.2);
        return quotationItem;
    }

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
        store.setColorPalette(ColorPalette.CORESTREET);
        return store;
    }

    //ProductVariant
    public static ProductVariant createTestProductVariantA() {
        ProductVariant variant = new ProductVariant();
        variant.setSize(Size.M);
        variant.setColor(Color.RED);
        variant.setStock(10);
        return variant;
    }

    public static ProductVariant createTestProductVariantB() {
        ProductVariant variant = new ProductVariant();
        variant.setSize(Size.L);
        variant.setColor(Color.BLUE);
        variant.setStock(5);
        return variant;
    }

    public static ProductVariant createTestProductVariantC() {
        ProductVariant variant = new ProductVariant();
        variant.setSize(Size.XL);
        variant.setColor(Color.GREEN);
        variant.setStock(40);
        return variant;
    }
}
