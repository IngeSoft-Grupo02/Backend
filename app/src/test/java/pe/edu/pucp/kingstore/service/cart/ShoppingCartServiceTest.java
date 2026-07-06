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
    void addItemCreatesSeparateLineWhenRequestedForSameVariant() {
        Customer customer = customer(1);
        Store store = store(10);
        Product product = product(1, store, 100.0);
        ProductVariant variant = variant(1, product, 50);
        ShoppingCart cart = emptyCart(customer);

        CartItem existingItem = new CartItem();
        existingItem.setProductVariant(variant);
        existingItem.setQuantity(3);
        existingItem.setPrice(100.0);
        existingItem.setSubtotal(300.0);
        cart.getItems().add(existingItem);

        when(discountRepository.findByStoreId(10)).thenReturn(List.of());
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.addItem(cart, variant, 2, 10, true);

        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems()).extracting(CartItem::getQuantity)
                .containsExactly(3, 2);
    }

    @Test
    void addItemDoesNotMergePlainItemIntoDesignedLine() {
        Customer customer = customer(1);
        Store store = store(10);
        Product product = product(1, store, 100.0);
        ProductVariant variant = variant(1, product, 50);
        ShoppingCart cart = emptyCart(customer);

        CustomDesign design = new CustomDesign();
        design.setImageUrl("__pending_cart_design__");

        CartItem designedItem = new CartItem();
        designedItem.setProductVariant(variant);
        designedItem.setQuantity(3);
        designedItem.setPrice(110.0);
        designedItem.setSubtotal(330.0);
        designedItem.setCustomDesign(design);
        cart.getItems().add(designedItem);

        when(discountRepository.findByStoreId(10)).thenReturn(List.of());
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.addItem(cart, variant, 2, 10);

        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems().get(0).getCustomDesign()).isNotNull();
        assertThat(result.getItems().get(1).getCustomDesign()).isNull();
        assertThat(result.getItems()).extracting(CartItem::getQuantity)
                .containsExactly(3, 2);
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

        assertThat(result.getItems().get(0).getPrice()).isEqualTo(100.0);
        assertThat(result.getSubTotal()).isEqualTo(500.0);
        assertThat(result.getDiscount()).isEqualTo(50.0);
        assertThat(result.getTotalAmount()).isEqualTo(450.0);
    }

    @Test
    void addItemAppliesOpenEndedDiscountAtMinQuantityAndAbove() {
        Store store = store(10);
        Product product = product(1, store, 100.0);
        ProductVariant variant = variant(1, product, 50);
        Discount discount = discount(null, 10, 10, 10.0, true);

        when(discountRepository.findByStoreId(10)).thenReturn(List.of(discount));
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart nine = service.addItem(emptyCart(customer(1)), variant, 9, 10);
        ShoppingCart ten = service.addItem(emptyCart(customer(1)), variant, 10, 10);
        ShoppingCart eleven = service.addItem(emptyCart(customer(1)), variant, 11, 10);
        ShoppingCart twelve = service.addItem(emptyCart(customer(1)), variant, 12, 10);

        assertThat(nine.getItems().get(0).getPrice()).isEqualTo(100.0);
        assertThat(nine.getDiscount()).isZero();
        assertThat(ten.getItems().get(0).getPrice()).isEqualTo(100.0);
        assertThat(ten.getDiscount()).isEqualTo(100.0);
        assertThat(eleven.getItems().get(0).getPrice()).isEqualTo(100.0);
        assertThat(eleven.getDiscount()).isEqualTo(110.0);
        assertThat(twelve.getItems().get(0).getPrice()).isEqualTo(100.0);
        assertThat(twelve.getDiscount()).isEqualTo(120.0);
    }

    @Test
    void addItemRespectsRealUpperBoundWhenMaxQuantityIsGreaterThanMinQuantity() {
        Store store = store(10);
        Product product = product(1, store, 100.0);
        ProductVariant variant = variant(1, product, 50);
        Discount discount = discount(null, 10, 12, 10.0, true);

        when(discountRepository.findByStoreId(10)).thenReturn(List.of(discount));
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart inRange = service.addItem(emptyCart(customer(1)), variant, 12, 10);
        ShoppingCart outOfRange = service.addItem(emptyCart(customer(1)), variant, 13, 10);

        assertThat(inRange.getItems().get(0).getPrice()).isEqualTo(100.0);
        assertThat(inRange.getDiscount()).isEqualTo(120.0);
        assertThat(outOfRange.getItems().get(0).getPrice()).isEqualTo(100.0);
        assertThat(outOfRange.getDiscount()).isZero();
    }

    @Test
    void addItemRecalculatesDiscountUsingAccumulatedQuantity() {
        Store store = store(10);
        Product product = product(1, store, 100.0);
        ProductVariant variant = variant(1, product, 50);
        ShoppingCart cart = emptyCart(customer(1));
        CartItem existingItem = new CartItem();
        existingItem.setProductVariant(variant);
        existingItem.setQuantity(10);
        existingItem.setPrice(90.0);
        existingItem.setSubtotal(900.0);
        cart.getItems().add(existingItem);

        Discount discount = discount(null, 10, 10, 10.0, true);
        when(discountRepository.findByStoreId(10)).thenReturn(List.of(discount));
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.addItem(cart, variant, 1, 10);

        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(11);
        assertThat(result.getItems().get(0).getPrice()).isEqualTo(100.0);
        assertThat(result.getItems().get(0).getSubtotal()).isEqualTo(1100.0);
        assertThat(result.getDiscount()).isEqualTo(110.0);
        assertThat(result.getTotalAmount()).isEqualTo(990.0);
    }

    @Test
    void addItemDoesNotApplyPausedOrDeletedDiscounts() {
        Store store = store(10);
        Product product = product(1, store, 100.0);
        ProductVariant variant = variant(1, product, 50);
        Discount paused = discount(null, 1, 1, 90.0, false);
        Discount deleted = discount(null, 1, 1, 80.0, true);
        deleted.setDeleted(true);

        when(discountRepository.findByStoreId(10)).thenReturn(List.of(paused, deleted));
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.addItem(emptyCart(customer(1)), variant, 12, 10);

        assertThat(result.getItems().get(0).getPrice()).isEqualTo(100.0);
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

        assertThat(result.getItems().get(0).getPrice()).isEqualTo(100.0);
        assertThat(result.getDiscount()).isEqualTo(125.0);
        assertThat(result.getTotalAmount()).isEqualTo(375.0);
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
        item.setQuantity(1);
        item.setPrice(100.0);
        item.setSubtotal(100.0);
        cart.getItems().add(item);

        CustomDesignRequestDTO request = new CustomDesignRequestDTO();
        request.setDescription("Logo bordado en el pecho");

        when(discountRepository.findByStoreId(10)).thenReturn(List.of());
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.addDesignToItem(cart, 1, request);

        CartItem resultItem = result.getItems().get(0);
        assertThat(resultItem.getCustomDesign()).isNotNull();
        assertThat(resultItem.getCustomDesign().getDescription())
                .isEqualTo("Logo bordado en el pecho");
        assertThat(resultItem.getSubtotal()).isEqualTo(100.0);
    }

    @Test
    void addDesignToItemAppliesDesignFeeOnlyToDesignedItem() {
        Store store = store(10);
        Product designedProduct = product(1, store, 100.0);
        Product plainProduct = product(2, store, 50.0);
        ProductVariant designedVariant = variant(1, designedProduct, 50);
        ProductVariant plainVariant = variant(2, plainProduct, 50);
        ShoppingCart cart = emptyCart(customer(1));

        CartItem designedItem = new CartItem();
        designedItem.setId(1);
        designedItem.setProductVariant(designedVariant);
        designedItem.setQuantity(2);
        designedItem.setPrice(100.0);
        designedItem.setSubtotal(200.0);
        cart.getItems().add(designedItem);

        CartItem plainItem = new CartItem();
        plainItem.setId(2);
        plainItem.setProductVariant(plainVariant);
        plainItem.setQuantity(2);
        plainItem.setPrice(50.0);
        plainItem.setSubtotal(100.0);
        cart.getItems().add(plainItem);

        CustomDesignRequestDTO request = new CustomDesignRequestDTO();
        request.setDescription("Logo frontal");
        request.setImageUrl("https://cdn.test/logo.png");
        when(discountRepository.findByStoreId(10)).thenReturn(List.of());
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.addDesignToItem(cart, 1, request);

        assertThat(result.getItems().get(0).getSubtotal()).isEqualTo(220.0);
        assertThat(result.getItems().get(1).getSubtotal()).isEqualTo(100.0);
        assertThat(result.getTotalAmount()).isEqualTo(320.0);
    }

    @Test
    void addDesignToItemUsesStoreDesignFeePercentageFive() {
        Store store = store(10);
        store.setDesignFeePercentage(5.0);
        Product product = product(1, store, 100.0);
        ProductVariant variant = variant(1, product, 50);
        ShoppingCart cart = emptyCart(customer(1));

        CartItem item = new CartItem();
        item.setId(1);
        item.setProductVariant(variant);
        item.setQuantity(2);
        item.setPrice(100.0);
        item.setSubtotal(200.0);
        cart.getItems().add(item);

        CustomDesignRequestDTO request = new CustomDesignRequestDTO();
        request.setImageUrl("https://cdn.test/logo.png");
        when(discountRepository.findByStoreId(10)).thenReturn(List.of());
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.addDesignToItem(cart, 1, request);
        CartResponseDTO dto = service.toResponseDTO(result);

        assertThat(result.getItems().get(0).getSubtotal()).isEqualTo(210.0);
        assertThat(result.getTotalAmount()).isEqualTo(210.0);
        assertThat(dto.getDesignFeeTotal()).isEqualTo(10.0);
        assertThat(dto.getDesignFeePercentage()).isEqualTo(5.0);
        assertThat(dto.getItems().get(0).getDesignFeePercentage()).isEqualTo(5.0);
    }

    @Test
    void addDesignToItemUsesStoreDesignFeePercentageFifteen() {
        Store store = store(10);
        store.setDesignFeePercentage(15.0);
        Product product = product(1, store, 100.0);
        ProductVariant variant = variant(1, product, 50);
        ShoppingCart cart = emptyCart(customer(1));

        CartItem item = new CartItem();
        item.setId(1);
        item.setProductVariant(variant);
        item.setQuantity(2);
        item.setPrice(100.0);
        item.setSubtotal(200.0);
        cart.getItems().add(item);

        CustomDesignRequestDTO request = new CustomDesignRequestDTO();
        request.setImageUrl("https://cdn.test/logo.png");
        when(discountRepository.findByStoreId(10)).thenReturn(List.of());
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.addDesignToItem(cart, 1, request);

        assertThat(result.getItems().get(0).getSubtotal()).isEqualTo(230.0);
        assertThat(result.getTotalAmount()).isEqualTo(230.0);
    }

    @Test
    void addDesignToItemKeepsQuantityDiscountBasedOnBaseSubtotal() {
        Store store = store(10);
        store.setDesignFeePercentage(15.0);
        Product product = product(1, store, 100.0);
        ProductVariant variant = variant(1, product, 50);
        ShoppingCart cart = emptyCart(customer(1));

        CartItem item = new CartItem();
        item.setId(1);
        item.setProductVariant(variant);
        item.setQuantity(2);
        item.setPrice(100.0);
        item.setSubtotal(200.0);
        cart.getItems().add(item);

        Discount discount = discount(null, 2, 2, 10.0, true);
        CustomDesignRequestDTO request = new CustomDesignRequestDTO();
        request.setImageUrl("https://cdn.test/logo.png");
        when(discountRepository.findByStoreId(10)).thenReturn(List.of(discount));
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart result = service.addDesignToItem(cart, 1, request);

        assertThat(result.getSubTotal()).isEqualTo(230.0);
        assertThat(result.getDiscount()).isEqualTo(20.0);
        assertThat(result.getTotalAmount()).isEqualTo(210.0);
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
        item.setQuantity(1);
        item.setPrice(100.0);
        item.setSubtotal(100.0);
        item.setCustomDesign(existingDesign);
        cart.getItems().add(item);

        assertThatThrownBy(() -> service.addDesignToItem(cart, 1, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Design request is required");

        CustomDesignRequestDTO request = new CustomDesignRequestDTO();
        request.setImageUrl("https://cdn.test/design.png");
        request.setDescription("Nuevo diseño");
        when(discountRepository.findByStoreId(10)).thenReturn(List.of());
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
        store.setDesignFeePercentage(15.0);
        Product product = product(1, store, 100.0);
        product.setImageUrls(List.of("https://cdn.test/producto-1.png"));
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
        assertThat(result.getItems().get(0).getProductImageUrl()).isEqualTo("https://cdn.test/producto-1.png");
        assertThat(result.getTotalAmount()).isEqualTo(100.0);
        assertThat(result.getDesignFeePercentage()).isEqualTo(15.0);
        assertThat(result.getItems().get(0).getDesignFeePercentage()).isEqualTo(15.0);
        assertThat(result.getDesignFeeTotal()).isZero();
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

        ShoppingCart valid = emptyCart(customer(1));
        valid.setDiscount(2.0);
        valid.getItems().add(cartItem(2, 10.0));
        when(shoppingCartRepository.save(any(ShoppingCart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShoppingCart saved = service.create(valid);

        assertThat(saved.getSubTotal()).isEqualTo(20.0);
        assertThat(saved.getDiscount()).isZero();
        assertThat(saved.getTotalAmount()).isEqualTo(20.0);
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
