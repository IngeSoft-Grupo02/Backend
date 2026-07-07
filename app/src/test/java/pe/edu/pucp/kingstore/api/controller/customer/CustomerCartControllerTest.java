package pe.edu.pucp.kingstore.api.controller.customer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import pe.edu.pucp.kingstore.api.context.CustomerContext;
import pe.edu.pucp.kingstore.domain.dto.cart.CartItemRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.cart.CartResponseDTO;
import pe.edu.pucp.kingstore.domain.dto.product.CustomDesignRequestDTO;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.service.cart.ShoppingCartService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.product.ProductVariantService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerCartControllerTest {

    @Mock private CustomerContext customerContext;
    @Mock private ProductVariantService productVariantService;
    @Mock private ShoppingCartService shoppingCartService;

    private CustomerCartController controller;
    private Authentication authentication;
    private Store store;
    private Customer customer;
    private ShoppingCart cart;
    private CartResponseDTO cartResponseDTO;

    @BeforeEach
    void setUp() {
        controller = new CustomerCartController(customerContext, productVariantService, shoppingCartService);
        authentication = mock(Authentication.class);
        store = new Store();
        store.setId(10);
        customer = new Customer();
        customer.setId(1);
        cart = new ShoppingCart();
        cart.setId(1);
        cartResponseDTO = new CartResponseDTO(1, List.of(), 0, 0, 0);
    }

    // ── GET /stores/{slug}/cart ───────────────────────────────────────────────

    @Test
    void getCartReturnsCartForAuthenticatedCustomer() {
        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(shoppingCartService.getOrCreateCart(customer)).thenReturn(cart);
        when(shoppingCartService.toResponseDTO(cart)).thenReturn(cartResponseDTO);

        var result = controller.getCart("tienda-luna", authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isEqualTo(cartResponseDTO);
    }

    @Test
    void getCartReturnsBadRequestOnBusinessRuleException() {
        when(customerContext.store("tienda-luna"))
                .thenThrow(new BusinessRuleException("Store not found"));

        var result = controller.getCart("tienda-luna", authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── POST /stores/{slug}/cart/items ────────────────────────────────────────

    @Test
    void addItemReturnsUpdatedCart() {
        ProductVariant variant = new ProductVariant();
        variant.setId(1);
        CartItemRequestDTO request = new CartItemRequestDTO();
        request.setProductVariantId(1);
        request.setQuantity(2);

        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(productVariantService.getByIdWithProduct(1)).thenReturn(variant);
        when(shoppingCartService.getOrCreateCart(customer)).thenReturn(cart);
        when(shoppingCartService.addItem(cart, variant, 2, 10, false, null)).thenReturn(cart);
        when(shoppingCartService.toResponseDTO(cart)).thenReturn(cartResponseDTO);

        var result = controller.addItem("tienda-luna", authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void addItemPassesSeparateItemWhenRequested() {
        ProductVariant variant = new ProductVariant();
        variant.setId(1);
        CartItemRequestDTO request = new CartItemRequestDTO();
        request.setProductVariantId(1);
        request.setQuantity(2);
        request.setSeparateItem(true);

        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(productVariantService.getByIdWithProduct(1)).thenReturn(variant);
        when(shoppingCartService.getOrCreateCart(customer)).thenReturn(cart);
        when(shoppingCartService.addItem(cart, variant, 2, 10, true, null)).thenReturn(cart);
        when(shoppingCartService.toResponseDTO(cart)).thenReturn(cartResponseDTO);

        var result = controller.addItem("tienda-luna", authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void addItemPassesSelectedProductImageWhenProvided() {
        ProductVariant variant = new ProductVariant();
        variant.setId(1);
        CartItemRequestDTO request = new CartItemRequestDTO();
        request.setProductVariantId(1);
        request.setQuantity(2);
        request.setSelectedProductImageUrl("https://cdn.test/product-2.png");

        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(productVariantService.getByIdWithProduct(1)).thenReturn(variant);
        when(shoppingCartService.getOrCreateCart(customer)).thenReturn(cart);
        when(shoppingCartService.addItem(cart, variant, 2, 10, false, "https://cdn.test/product-2.png")).thenReturn(cart);
        when(shoppingCartService.toResponseDTO(cart)).thenReturn(cartResponseDTO);

        var result = controller.addItem("tienda-luna", authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void addItemReturnsBadRequestWhenVariantIdNull() {
        CartItemRequestDTO request = new CartItemRequestDTO();
        request.setProductVariantId(null);
        request.setQuantity(2);

        var result = controller.addItem("tienda-luna", authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void addItemReturnsBadRequestWhenQuantityZero() {
        CartItemRequestDTO request = new CartItemRequestDTO();
        request.setProductVariantId(1);
        request.setQuantity(0);

        var result = controller.addItem("tienda-luna", authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── PUT /stores/{slug}/cart/items/{itemId} ────────────────────────────────

    @Test
    void updateItemReturnsUpdatedCart() {
        CartItemRequestDTO request = new CartItemRequestDTO();
        request.setQuantity(3);

        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(shoppingCartService.getOrCreateCart(customer)).thenReturn(cart);
        when(shoppingCartService.updateItem(cart, 1, 3, 10)).thenReturn(cart);
        when(shoppingCartService.toResponseDTO(cart)).thenReturn(cartResponseDTO);

        var result = controller.updateItem("tienda-luna", 1, authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateItemReturnsBadRequestWhenQuantityNull() {
        CartItemRequestDTO request = new CartItemRequestDTO();
        request.setQuantity(null);

        var result = controller.updateItem("tienda-luna", 1, authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── DELETE /stores/{slug}/cart/items/{itemId} ─────────────────────────────

    @Test
    void removeItemReturnsUpdatedCart() {
        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(shoppingCartService.getOrCreateCart(customer)).thenReturn(cart);
        when(shoppingCartService.removeItem(cart, 1)).thenReturn(cart);
        when(shoppingCartService.toResponseDTO(cart)).thenReturn(cartResponseDTO);

        var result = controller.removeItem("tienda-luna", 1, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void removeItemReturnsBadRequestWhenItemNotFound() {
        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(shoppingCartService.getOrCreateCart(customer)).thenReturn(cart);
        when(shoppingCartService.removeItem(cart, 99))
                .thenThrow(new ResourceNotFoundException("Cart item", 99));

        var result = controller.removeItem("tienda-luna", 99, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── PATCH /stores/{slug}/cart/items/{itemId}/design ───────────────────────

    @Test
    void addDesignReturnsUpdatedCart() {
        CustomDesignRequestDTO request = new CustomDesignRequestDTO();
        request.setDescription("Logo en el pecho");

        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(shoppingCartService.getOrCreateCart(customer)).thenReturn(cart);
        when(shoppingCartService.addDesignToItem(cart, 1, request)).thenReturn(cart);
        when(shoppingCartService.toResponseDTO(cart)).thenReturn(cartResponseDTO);

        var result = controller.addDesign("tienda-luna", 1, authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void addDesignReturnsBadRequestOnBusinessRuleException() {
        CustomDesignRequestDTO request = new CustomDesignRequestDTO();

        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(shoppingCartService.getOrCreateCart(customer)).thenReturn(cart);
        when(shoppingCartService.addDesignToItem(cart, 1, request))
                .thenThrow(new BusinessRuleException("Design must have an image or a description"));

        var result = controller.addDesign("tienda-luna", 1, authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
