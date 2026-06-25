package pe.edu.pucp.kingstore.service.cart;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.pucp.kingstore.domain.dto.cart.CartResponseDTO;
import pe.edu.pucp.kingstore.domain.dto.product.CustomDesignRequestDTO;
import pe.edu.pucp.kingstore.domain.model.cart.CartItem;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.domain.model.product.Discount;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import pe.edu.pucp.kingstore.domain.model.product.enums.VolumeType;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.repository.cart.ShoppingCartRepository;
import pe.edu.pucp.kingstore.repository.product.DiscountRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShoppingCartServiceTest {

    @Mock private ShoppingCartRepository shoppingCartRepository;
    @Mock private DiscountRepository discountRepository;

    private ShoppingCartService service;

    @BeforeEach
    void setUp() {
        service = new ShoppingCartService(shoppingCartRepository, discountRepository);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Store store(Integer id) {
        Store store = new Store();
        store.setId(id);
        return store;
    }

    private Product product(Integer id, Store store, double basePrice) {
        Product product = new Product();
        product.setId(id);
        product.setName("Producto " + id);
        product.setBasePrice(basePrice);
        product.setStore(store);
        return product;
    }

    private ProductVariant variant(Integer id, Product product, int stock) {
        ProductVariant variant = new ProductVariant();
        variant.setId(id);
        variant.setSize("M");
        variant.setColor(Color.BLACK);
        variant.setStock(stock);
        variant.setProduct(product);
        return variant;
    }

    private Customer customer(Integer id) {
        Customer customer = new Customer();
        customer.setId(id);
        return customer;
    }

    private ShoppingCart emptyCart(Customer customer) {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(1);
        cart.setCustomer(customer);
        cart.setItems(new ArrayList<>());
        cart.setSubTotal(0);
        cart.setDiscount(0);
        cart.setTotalAmount(0);
        return cart;
    }

    // ── getOrCreateCart ───────────────────────────────────────────────────────

    @Test
    void getOrCreateCartReturnsExistingCart() {
        Customer customer = customer(1);
        ShoppingCart existing = emptyCart(customer);
        when(shoppingCartRepository.findByCustomerIdAndActiveTrueOrderByIdDesc(1)).thenReturn(List.of(existing));

        ShoppingCart result = service.getOrCreateCart(customer);

        assertThat(result).isSameAs(existing);
    }

    @Test
    void getOrCreateCartCreatesNewCartWhenNoneExists() {
        Customer customer = customer(1);
        when(shoppingCartRepository.findByCustomerIdAndActiveTrueOrderByIdDesc(1)).thenReturn(List.of());
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.getOrCreateCart(customer);

        assertThat(result.getCustomer()).isEqualTo(customer);
        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalAmount()).isEqualTo(0);
    }

    @Test
    void getOrCreateCartReturnsMostRecentActiveCartWhenCustomerHasHistoricalCarts() {
        Customer customer = customer(1);
        ShoppingCart older = emptyCart(customer);
        older.setId(10);
        ShoppingCart latest = emptyCart(customer);
        latest.setId(12);
        when(shoppingCartRepository.findByCustomerIdAndActiveTrueOrderByIdDesc(1))
                .thenReturn(List.of(latest, older));

        ShoppingCart result = service.getOrCreateCart(customer);

        assertThat(result).isSameAs(latest);
    }

    // ── addItem ───────────────────────────────────────────────────────────────

    @Test
    void addItemAddsNewItemToCart() {
        Customer customer = customer(1);
        Store store = store(10);
        Product product = product(1, store, 100.0);
        ProductVariant variant = variant(1, product, 50);
        ShoppingCart cart = emptyCart(customer);

        when(discountRepository.findByStoreId(10)).thenReturn(List.of());
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.addItem(cart, variant, 2, 10);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(result.getItems().get(0).getPrice()).isEqualTo(100.0);
    }

    @Test
    void addItemAccumulatesQuantityForExistingVariant() {
        Customer customer = customer(1);
        Store store = store(10);
        Product product = product(1, store, 100.0);
        ProductVariant variant = variant(1, product, 50);
        ShoppingCart cart = emptyCart(customer);

        // Agregar el mismo variant dos veces
        CartItem existingItem = new CartItem();
        existingItem.setProductVariant(variant);
        existingItem.setQuantity(3);
        existingItem.setPrice(100.0);
        existingItem.setSubtotal(300.0);
        cart.getItems().add(existingItem);

        when(discountRepository.findByStoreId(10)).thenReturn(List.of());
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.addItem(cart, variant, 2, 10);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void addItemThrowsWhenQuantityZero() {
        Customer customer = customer(1);
        Store store = store(10);
        Product product = product(1, store, 100.0);
        ProductVariant variant = variant(1, product, 50);
        ShoppingCart cart = emptyCart(customer);

        assertThatThrownBy(() -> service.addItem(cart, variant, 0, 10))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Quantity must be positive");
    }

    @Test
    void addItemAllowsQuantityAboveAvailableStock() {
        // Regla de negocio: el stock no bloquea la cotización. El cliente puede
        // solicitar más unidades de las disponibles y el comerciante decide.
        Customer customer = customer(1);
        Store store = store(10);
        Product product = product(1, store, 100.0);
        ProductVariant variant = variant(1, product, 3);
        ShoppingCart cart = emptyCart(customer);

        when(discountRepository.findByStoreId(10)).thenReturn(List.of());
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.addItem(cart, variant, 5, 10);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(5);
    }

    @Test
    void addItemAppliesDiscountWhenQuantityMeetsThreshold() {
        Customer customer = customer(1);
        Store store = store(10);
        Product product = product(1, store, 100.0);
        ProductVariant variant = variant(1, product, 50);
        ShoppingCart cart = emptyCart(customer);

        Discount discount = new Discount();
        discount.setId(1);
        discount.setMinQuantity(5);
        discount.setMaxQuantity(20);
        discount.setDiscountPercentage(10.0);
        discount.setActive(true);
        discount.setProduct(null); // aplica a todo el catálogo

        when(discountRepository.findByStoreId(10)).thenReturn(List.of(discount));
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.addItem(cart, variant, 5, 10);

        // 100 - 10% = 90
        assertThat(result.getItems().get(0).getPrice()).isEqualTo(90.0);
    }

    // ── updateItem ────────────────────────────────────────────────────────────

    @Test
    void updateItemChangesQuantityAndRecalculates() {
        Customer customer = customer(1);
        Store store = store(10);
        Product product = product(1, store, 100.0);
        ProductVariant variant = variant(1, product, 50);
        ShoppingCart cart = emptyCart(customer);

        CartItem item = new CartItem();
        item.setId(1);
        item.setProductVariant(variant);
        item.setQuantity(2);
        item.setPrice(100.0);
        item.setSubtotal(200.0);
        cart.getItems().add(item);

        when(discountRepository.findByStoreId(10)).thenReturn(List.of());
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.updateItem(cart, 1, 5, 10);

        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(5);
        assertThat(result.getItems().get(0).getSubtotal()).isEqualTo(500.0);
    }

    @Test
    void updateItemAllowsQuantityAboveAvailableStock() {
        // Mismo criterio que addItem: el stock no bloquea la cotización.
        Customer customer = customer(1);
        Store store = store(10);
        Product product = product(1, store, 100.0);
        ProductVariant variant = variant(1, product, 3);
        ShoppingCart cart = emptyCart(customer);

        CartItem item = new CartItem();
        item.setId(1);
        item.setProductVariant(variant);
        item.setQuantity(2);
        item.setPrice(100.0);
        item.setSubtotal(200.0);
        cart.getItems().add(item);

        when(discountRepository.findByStoreId(10)).thenReturn(List.of());
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.updateItem(cart, 1, 50, 10);

        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(50);
    }

    @Test
    void updateItemThrowsWhenItemNotFound() {
        ShoppingCart cart = emptyCart(customer(1));

        assertThatThrownBy(() -> service.updateItem(cart, 99, 2, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateItemThrowsWhenQuantityZero() {
        Customer customer = customer(1);
        Store store = store(10);
        Product product = product(1, store, 100.0);
        ProductVariant variant = variant(1, product, 50);
        ShoppingCart cart = emptyCart(customer);

        CartItem item = new CartItem();
        item.setId(1);
        item.setProductVariant(variant);
        item.setQuantity(2);
        cart.getItems().add(item);

        assertThatThrownBy(() -> service.updateItem(cart, 1, 0, 10))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ── removeItem ────────────────────────────────────────────────────────────

    @Test
    void removeItemDeletesItemFromCart() {
        ShoppingCart cart = emptyCart(customer(1));

        CartItem item = new CartItem();
        item.setId(1);
        cart.getItems().add(item);

        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.removeItem(cart, 1);

        assertThat(result.getItems()).isEmpty();
    }

    @Test
    void removeItemThrowsWhenItemNotFound() {
        ShoppingCart cart = emptyCart(customer(1));

        assertThatThrownBy(() -> service.removeItem(cart, 99))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── addDesignToItem ───────────────────────────────────────────────────────

    @Test
    void addDesignToItemRegistersDesignOnCartItem() {
        Customer customer = customer(1);
        Store store = store(10);
        Product product = product(1, store, 100.0);
        ProductVariant variant = variant(1, product, 50);
        ShoppingCart cart = emptyCart(customer);

        CartItem item = new CartItem();
        item.setId(1);
        item.setProductVariant(variant);
        cart.getItems().add(item);

        CustomDesignRequestDTO request = new CustomDesignRequestDTO();
        request.setDescription("Logo bordado en el pecho");

        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.addDesignToItem(cart, 1, request);

        assertThat(result.getItems().get(0).getCustomDesign()).isNotNull();
        assertThat(result.getItems().get(0).getCustomDesign().getDescription())
                .isEqualTo("Logo bordado en el pecho");
    }

    @Test
    void addDesignToItemThrowsWhenRequestEmpty() {
        ShoppingCart cart = emptyCart(customer(1));
        CartItem item = new CartItem();
        item.setId(1);
        cart.getItems().add(item);

        CustomDesignRequestDTO emptyRequest = new CustomDesignRequestDTO();

        assertThatThrownBy(() -> service.addDesignToItem(cart, 1, emptyRequest))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("image or a description");
    }

    @Test
    void addDesignToItemThrowsWhenItemNotFound() {
        ShoppingCart cart = emptyCart(customer(1));

        CustomDesignRequestDTO request = new CustomDesignRequestDTO();
        request.setDescription("test");

        assertThatThrownBy(() -> service.addDesignToItem(cart, 99, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── toResponseDTO ─────────────────────────────────────────────────────────

    @Test
    void toResponseDTOMapsCartCorrectly() {
        Customer customer = customer(1);
        Store store = store(10);
        Product product = product(1, store, 100.0);
        ProductVariant variant = variant(1, product, 50);
        ShoppingCart cart = emptyCart(customer);
        cart.setSubTotal(100.0);
        cart.setDiscount(0.0);
        cart.setTotalAmount(100.0);

        CartItem item = new CartItem();
        item.setId(1);
        item.setProductVariant(variant);
        item.setQuantity(1);
        item.setPrice(100.0);
        item.setSubtotal(100.0);
        cart.getItems().add(item);

        CartResponseDTO result = service.toResponseDTO(cart);

        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getProductName()).isEqualTo("Producto 1");
        assertThat(result.getTotalAmount()).isEqualTo(100.0);
    }

    @Test
    void toResponseDTOHandlesEmptyCart() {
        ShoppingCart cart = emptyCart(customer(1));

        CartResponseDTO result = service.toResponseDTO(cart);

        assertThat(result.getItems()).isEmpty();
        assertThat(result.getTotalAmount()).isEqualTo(0);
    }
}
