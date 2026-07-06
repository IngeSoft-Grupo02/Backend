package pe.edu.pucp.kingstore.api.controller.customer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import pe.edu.pucp.kingstore.api.context.CustomerContext;
import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.domain.dto.order.ShippingAddressRequestDTO;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.order.OrderService;
import pe.edu.pucp.kingstore.domain.dto.order.OrderResponseDTO;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerOrderControllerTest {

    @Mock private CustomerContext customerContext;
    @Mock private OrderService    orderService;

    private CustomerOrderController controller;
    private Authentication authentication;
    private Store store;
    private Customer customer;
    private Order order;

    @BeforeEach
    void setUp() {
        controller     = new CustomerOrderController(customerContext, orderService);
        authentication = mock(Authentication.class);

        store = new Store();
        store.setId(10);

        customer = new Customer();
        customer.setId(1);

        order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
        order.setFinalTotal(200.0);
    }

    // ── GET /stores/{slug}/orders ─────────────────────────────────────────────

    @Test
    void findAllReturnsCustomerOrders() {
        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(orderService.findByCustomerAndStore(1, 10)).thenReturn(List.of(order));
        when(orderService.toResponseDTO(order, 10)).thenReturn(new OrderResponseDTO());

        var result = controller.findAll("tienda-luna", authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<?> body = (List<?>) result.getBody();
        assertThat(body).hasSize(1);
    }

    @Test
    void findAllReturnsEmptyListWhenNoOrders() {
        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(orderService.findByCustomerAndStore(1, 10)).thenReturn(List.of());

        var result = controller.findAll("tienda-luna", authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<?> body = (List<?>) result.getBody();
        assertThat(body).isEmpty();
    }

    @Test
    void findAllReturnsBadRequestOnBusinessRuleException() {
        when(customerContext.store("tienda-luna"))
                .thenThrow(new BusinessRuleException("Store not found"));

        var result = controller.findAll("tienda-luna", authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── GET /stores/{slug}/orders/{id} ────────────────────────────────────────

    @Test
    void findByIdReturnsOrder() {
        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(orderService.findByCustomerInStore(1, 1, 10)).thenReturn(order);
        when(orderService.toResponseDTO(order, 10)).thenReturn(new OrderResponseDTO());

        var result = controller.findById("tienda-luna", 1, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void findByIdReturns404WhenNotFound() {
        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(orderService.findByCustomerInStore(99, 1, 10))
                .thenThrow(new ResourceNotFoundException("Order", 99));

        var result = controller.findById("tienda-luna", 99, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ── PUT /stores/{slug}/orders/{id}/shipping-address ───────────────────────

    private ShippingAddressRequestDTO shippingRequest() {
        var dto = new ShippingAddressRequestDTO();
        dto.setAddress("Av. Larco 123");
        dto.setDistrict("MIRAFLORES");
        dto.setReference("Frente al parque");
        dto.setRecipientName("Ana García");
        dto.setPhone("987654321");
        return dto;
    }

    @Test
    void setShippingAddressReturnsOk() {
        var request = shippingRequest();
        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(orderService.findByCustomerInStore(1, 1, 10)).thenReturn(order);
        when(orderService.setShippingAddress(1, request)).thenReturn(order);
        when(orderService.toResponseDTO(order, 10)).thenReturn(new OrderResponseDTO());

        var result = controller.setShippingAddress("tienda-luna", 1, authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void setShippingAddressReturns404WhenOrderNotFound() {
        var request = shippingRequest();
        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(orderService.findByCustomerInStore(99, 1, 10))
                .thenThrow(new ResourceNotFoundException("Order", 99));

        var result = controller.setShippingAddress("tienda-luna", 99, authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void setShippingAddressReturnsBadRequestOnBusinessRule() {
        var request = shippingRequest();
        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(orderService.findByCustomerInStore(1, 1, 10)).thenReturn(order);
        when(orderService.setShippingAddress(1, request))
                .thenThrow(new BusinessRuleException("Order is cancelled"));

        var result = controller.setShippingAddress("tienda-luna", 1, authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void setShippingAddressReturnsBadRequestWhenStoreNotFound() {
        var request = shippingRequest();
        when(customerContext.store("tienda-luna"))
                .thenThrow(new BusinessRuleException("Store not found"));

        var result = controller.setShippingAddress("tienda-luna", 1, authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}