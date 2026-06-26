package pe.edu.pucp.kingstore.api.controller.customer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import pe.edu.pucp.kingstore.api.context.CustomerContext;
import pe.edu.pucp.kingstore.domain.dto.quotation.QuotationResponseDTO;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.service.cart.ShoppingCartService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.quotation.QuotationService;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerQuotationControllerTest {

    @Mock private CustomerContext     customerContext;
    @Mock private ShoppingCartService shoppingCartService;
    @Mock private QuotationService    quotationService;
    private CustomerQuotationController controller;
    private Authentication authentication;
    private Store store;
    private Customer customer;
    private ShoppingCart cart;
    private Quotation quotation;
    private QuotationResponseDTO responseDTO;

    @BeforeEach
    void setUp() {
        controller     = new CustomerQuotationController(customerContext, shoppingCartService, quotationService);
        authentication = mock(Authentication.class);

        store = new Store();
        store.setId(10);

        customer = new Customer();
        customer.setId(1);

        cart = new ShoppingCart();
        cart.setId(1);

        quotation = new Quotation();
        quotation.setId(1);
        quotation.setStatus(QuotationStatus.PENDING);

        responseDTO = new QuotationResponseDTO();
        responseDTO.setId(1);
        responseDTO.setStatus(QuotationStatus.PENDING);
        responseDTO.setStatusLabel("Pendiente");
    }

    // ── POST /stores/{slug}/quotations ────────────────────────────────────────

    @Test
    void createReturns201WithQuotation() {
        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(shoppingCartService.getOrCreateCart(customer)).thenReturn(cart);
        when(quotationService.createFromCart(cart)).thenReturn(quotation);
        when(quotationService.toResponseDTO(quotation, 10)).thenReturn(responseDTO);

        var result = controller.create("tienda-luna", authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(((QuotationResponseDTO) result.getBody()).getId()).isEqualTo(1);
    }

    @Test
    void createReturnsBadRequestOnBusinessRuleException() {
        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(shoppingCartService.getOrCreateCart(customer)).thenReturn(cart);
        when(quotationService.createFromCart(cart))
                .thenThrow(new BusinessRuleException("Cart must have at least one item"));

        var result = controller.create("tienda-luna", authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void createDoesNotDeactivateCartWhenCreationFails() {
        // El carrito SOLO se desactiva tras crear la cotización con éxito. Si
        // createFromCart lanza (carrito vacío o, en una carrera, ya cotizado), no
        // debe destruirse el carrito: nada de deactivate y respuesta 400.
        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(shoppingCartService.getOrCreateCart(customer)).thenReturn(cart);
        when(quotationService.createFromCart(cart))
                .thenThrow(new BusinessRuleException("Cart already has a quotation"));

        var result = controller.create("tienda-luna", authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(shoppingCartService, never()).deactivate(anyInt());
    }

    @Test
    void createDeactivatesCartAfterSuccessfulQuotationCreation() {
        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(shoppingCartService.getOrCreateCart(customer)).thenReturn(cart);
        when(quotationService.createFromCart(cart)).thenReturn(quotation);
        when(quotationService.toResponseDTO(quotation, 10)).thenReturn(responseDTO);

        controller.create("tienda-luna", authentication);

        verify(shoppingCartService).deactivate(1);
    }

    // ── GET /stores/{slug}/quotations ─────────────────────────────────────────

    @Test
    void findAllReturnsCustomerQuotations() {
        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(quotationService.findByCustomerAndStore(1, 10)).thenReturn(List.of(quotation));
        when(quotationService.toResponseDTO(quotation, 10)).thenReturn(responseDTO);

        var result = controller.findAll("tienda-luna", authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<QuotationResponseDTO> body = (List<QuotationResponseDTO>) result.getBody();
        assertThat(body).hasSize(1);
    }

    @Test
    void findAllReturnsEmptyListWhenNoQuotations() {
        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(quotationService.findByCustomerAndStore(1, 10)).thenReturn(List.of());

        var result = controller.findAll("tienda-luna", authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<QuotationResponseDTO> body = (List<QuotationResponseDTO>) result.getBody();
        assertThat(body).isEmpty();
    }

    // ── GET /stores/{slug}/quotations/{id} ────────────────────────────────────

    @Test
    void findByIdReturnsQuotation() {
        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(quotationService.findByCustomerInStore(1, 1, 10)).thenReturn(quotation);
        when(quotationService.toResponseDTO(quotation, 10)).thenReturn(responseDTO);

        var result = controller.findById("tienda-luna", 1, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void findByIdReturns404WhenNotFound() {
        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(quotationService.findByCustomerInStore(99, 1, 10))
                .thenThrow(new ResourceNotFoundException("Quotation", 99));

        var result = controller.findById("tienda-luna", 99, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}