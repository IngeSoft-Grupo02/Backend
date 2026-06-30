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
import pe.edu.pucp.kingstore.domain.model.product.CustomDesign;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import pe.edu.pucp.kingstore.domain.model.product.enums.VolumeType;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.repository.cart.ShoppingCartRepository;
import pe.edu.pucp.kingstore.repository.product.DiscountRepository;
import pe.edu.pucp.kingstore.repository.quotation.QuotationRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShoppingCartServiceTest {

    @Mock private ShoppingCartRepository shoppingCartRepository;
    @Mock private DiscountRepository discountRepository;
    @Mock private QuotationRepository quotationRepository;

    private ShoppingCartService service;

    @BeforeEach
    void setUp() {
        service = new ShoppingCartService(shoppingCartRepository, discountRepository, quotationRepository);
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
        when(quotationRepository.findByShoppingCartId(1)).thenReturn(Optional.empty());

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
        when(quotationRepository.findByShoppingCartId(12)).thenReturn(Optional.empty());

        ShoppingCart result = service.getOrCreateCart(customer);

        assertThat(result).isSameAs(latest);
    }

    @Test
    void getOrCreateCartDeactivatesQuotedActiveCartAndCreatesNew() {
        // Carrito activo PERO ya cotizado (estado inconsistente histórico): debe
        // desactivarse y devolverse uno nuevo y vacío. El cliente no queda atrapado.
        Customer customer = customer(1);
        ShoppingCart quoted = emptyCart(customer);
        quoted.setId(5);
        quoted.setActive(true);
        when(shoppingCartRepository.findByCustomerIdAndActiveTrueOrderByIdDesc(1))
                .thenReturn(List.of(quoted));
        when(quotationRepository.findByShoppingCartId(5)).thenReturn(Optional.of(new Quotation()));
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.getOrCreateCart(customer);

        assertThat(quoted.getActive()).isFalse();
        assertThat(result).isNotSameAs(quoted);
        assertThat(result.getItems()).isEmpty();
        assertThat(result.getCustomer()).isEqualTo(customer);
    }

    @Test
    void getOrCreateCartSkipsQuotedCartAndReturnsCleanActiveOne() {
        // Con varios carritos activos históricos: salta el ya cotizado (desactivándolo)
        // y devuelve el primero válido sin cotización, sin crear uno innecesario.
        Customer customer = customer(1);
        ShoppingCart quoted = emptyCart(customer);
        quoted.setId(12);
        quoted.setActive(true);
        ShoppingCart clean = emptyCart(customer);
        clean.setId(10);
        when(shoppingCartRepository.findByCustomerIdAndActiveTrueOrderByIdDesc(1))
                .thenReturn(List.of(quoted, clean));
        when(quotationRepository.findByShoppingCartId(12)).thenReturn(Optional.of(new Quotation()));
        when(quotationRepository.findByShoppingCartId(10)).thenReturn(Optional.empty());
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.getOrCreateCart(customer);

        assertThat(result).isSameAs(clean);
        assertThat(quoted.getActive()).isFalse();
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

    @Test
    void addItemChoosesBestActiveApplicableDiscountOnly() {
        Customer customer = customer(1);
        Store store = store(10);
        Product product = product(1, store, 100.0);
        Product otherProduct = product(2, store, 100.0);
        ProductVariant variant = variant(1, product, 50);
        ShoppingCart cart = emptyCart(customer);

        Discount inactive = discount(null, 1, 100, 80.0, false);
        Discount otherProductDiscount = discount(otherProduct, 1, 100, 70.0, true);
        Discount outOfRange = discount(null, 20, 30, 60.0, true);
        Discount general = discount(null, 1, 10, 10.0, true);
        Discount specific = discount(product, 1, 10, 25.0, true);
        Discount deleted = discount(null, 1, 10, 95.0, true);
        deleted.setDeleted(true);

        when(discountRepository.findByStoreId(10))
                .thenReturn(List.of(inactive, otherProductDiscount, outOfRange, general, specific, deleted));
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.addItem(cart, variant, 5, 10);

        assertThat(result.getItems().get(0).getPrice()).isEqualTo(75.0);
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
    void addDesignToItemRejectsNullRequestAndReusesExistingDesign() {
        Customer customer = customer(1);
        Store store = store(10);
        Product product = product(1, store, 100.0);
        ProductVariant variant = variant(1, product, 50);
        ShoppingCart cart = emptyCart(customer);

        CustomDesign existingDesign = new CustomDesign();
        existingDesign.setId(77);
        CartItem item = new CartItem();
        item.setId(1);
        item.setProductVariant(variant);
        item.setCustomDesign(existingDesign);
        cart.getItems().add(item);

        assertThatThrownBy(() -> service.addDesignToItem(cart, 1, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Design request is required");

        CustomDesignRequestDTO request = new CustomDesignRequestDTO();
        request.setImageUrl("https://cdn.test/design.png");
        request.setDescription("Nuevo diseño");
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.addDesignToItem(cart, 1, request);

        assertThat(result.getItems().get(0).getCustomDesign()).isSameAs(existingDesign);
        assertThat(existingDesign.getImageUrl()).isEqualTo("https://cdn.test/design.png");
        assertThat(existingDesign.getProduct()).isSameAs(product);
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

    @Test
    void toResponseDTOHandlesNullItemsAndCustomDesignDetails() {
        ShoppingCart empty = emptyCart(customer(1));
        empty.setItems(null);
        assertThat(service.toResponseDTO(empty).getItems()).isEmpty();

        Product product = product(1, store(10), 0.0);
        ProductVariant variant = variant(1, product, 50);
        CustomDesign design = new CustomDesign();
        design.setId(9);
        design.setImageUrl("https://cdn.test/design.png");
        design.setDescription("Bordado");
        design.setObservations("OK");

        CartItem item = new CartItem();
        item.setId(1);
        item.setProductVariant(variant);
        item.setQuantity(1);
        item.setPrice(0.0);
        item.setSubtotal(0.0);
        item.setCustomDesign(design);

        ShoppingCart cart = emptyCart(customer(1));
        cart.getItems().add(item);

        CartResponseDTO result = service.toResponseDTO(cart);

        assertThat(result.getItems()).singleElement().satisfies(dto -> {
            assertThat(dto.getDiscountApplied()).isZero();
            assertThat(dto.getCustomDesign()).isNotNull();
            assertThat(dto.getCustomDesign().getImageUrl()).isEqualTo("https://cdn.test/design.png");
        });
    }

    @Test
    void createValidatesCustomerItemsPricesAndDiscount() {
        ShoppingCart noCustomer = new ShoppingCart();
        noCustomer.setItems(new ArrayList<>());
        noCustomer.setDiscount(0.0);
        assertThatThrownBy(() -> service.create(noCustomer))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Shopping cart must belong to a customer");

        ShoppingCart badQuantity = emptyCart(customer(1));
        badQuantity.getItems().add(cartItem(0, 10.0));
        assertThatThrownBy(() -> service.create(badQuantity))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cart item quantity must be positive");

        ShoppingCart badPrice = emptyCart(customer(1));
        badPrice.getItems().add(cartItem(1, -1.0));
        assertThatThrownBy(() -> service.create(badPrice))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cart item price cannot be negative");

        ShoppingCart badDiscount = emptyCart(customer(1));
        badDiscount.setDiscount(50.0);
        badDiscount.getItems().add(cartItem(1, 10.0));
        assertThatThrownBy(() -> service.create(badDiscount))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Cart discount must be between zero and subtotal");

        ShoppingCart valid = emptyCart(customer(1));
        valid.setDiscount(2.0);
        valid.getItems().add(cartItem(2, 10.0));
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart saved = service.create(valid);

        assertThat(saved.getSubTotal()).isEqualTo(20.0);
        assertThat(saved.getTotalAmount()).isEqualTo(18.0);
    }

    private Discount discount(Product product, int min, int max, double percentage, boolean active) {
        Discount discount = new Discount();
        discount.setProduct(product);
        discount.setMinQuantity(min);
        discount.setMaxQuantity(max);
        discount.setDiscountPercentage(percentage);
        discount.setActive(active);
        return discount;
    }

    private CartItem cartItem(int quantity, double price) {
        CartItem item = new CartItem();
        item.setQuantity(quantity);
        item.setPrice(price);
        return item;
    }
}
