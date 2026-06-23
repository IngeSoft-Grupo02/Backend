package pe.edu.pucp.kingstore.api.controller.customer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import pe.edu.pucp.kingstore.api.context.CustomerContext;
import pe.edu.pucp.kingstore.domain.dto.payment.PaymentRequestDTO;
import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;
import pe.edu.pucp.kingstore.domain.model.payment.PaymentReceipt;
import pe.edu.pucp.kingstore.domain.model.payment.enums.PaymentMethod;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.order.OrderService;
import pe.edu.pucp.kingstore.service.payment.PaymentReceiptService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerPaymentControllerTest {

    @Mock private CustomerContext       customerContext;
    @Mock private OrderService          orderService;
    @Mock private PaymentReceiptService paymentReceiptService;

    private CustomerPaymentController controller;
    private Authentication authentication;
    private Store store;
    private Customer customer;
    private Order order;

    @BeforeEach
    void setUp() {
        controller     = new CustomerPaymentController(customerContext, orderService, paymentReceiptService);
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

    private PaymentRequestDTO validRequest() {
        PaymentRequestDTO request = new PaymentRequestDTO();
        request.setRuc("20123456789");
        request.setPaymentMethod(PaymentMethod.VIRTUAL);
        request.setCardNumber("4111111111111111");
        request.setCardHolder("Juan Perez");
        request.setExpiryDate("12/27");
        request.setCvv("123");
        return request;
    }

    // ── POST /stores/{slug}/orders/{id}/payment ───────────────────────────────

    @Test
    void payReturns201WithReceiptInfo() {
        PaymentRequestDTO request = validRequest();
        PaymentReceipt receipt = new PaymentReceipt();
        receipt.setId(1);
        receipt.setFinalTotal(200.0);

        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(orderService.findByCustomerInStore(1, 1, 10)).thenReturn(order);
        when(paymentReceiptService.simulatePayment(
                order, "20123456789", PaymentMethod.VIRTUAL,
                "4111111111111111", "Juan Perez", "12/27", "123"))
                .thenReturn(receipt);

        var result = controller.pay("tienda-luna", 1, authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) result.getBody();
        assertThat(body.get("receiptId")).isEqualTo(1);
        assertThat(body.get("total")).isEqualTo(200.0);
        assertThat(body.get("paymentStatus")).isEqualTo("APPROVED");
    }

    @Test
    void payWorksWithNullRequest() {
        PaymentReceipt receipt = new PaymentReceipt();
        receipt.setId(1);
        receipt.setFinalTotal(200.0);

        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(orderService.findByCustomerInStore(1, 1, 10)).thenReturn(order);
        when(paymentReceiptService.simulatePayment(
                order, null, null, null, null, null, null))
                .thenReturn(receipt);

        var result = controller.pay("tienda-luna", 1, authentication, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void payReturnsBadRequestWhenCardDeclined() {
        PaymentRequestDTO request = validRequest();
        request.setCardNumber("4111111110000");

        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(orderService.findByCustomerInStore(1, 1, 10)).thenReturn(order);
        when(paymentReceiptService.simulatePayment(
                order, "20123456789", PaymentMethod.VIRTUAL,
                "4111111110000", "Juan Perez", "12/27", "123"))
                .thenThrow(new BusinessRuleException("Payment declined — card rejected by issuer"));

        var result = controller.pay("tienda-luna", 1, authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void payReturnsBadRequestWhenOrderNotFound() {
        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(orderService.findByCustomerInStore(99, 1, 10))
                .thenThrow(new ResourceNotFoundException("Order", 99));

        var result = controller.pay("tienda-luna", 99, authentication, validRequest());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void payReturnsBadRequestWhenAlreadyPaid() {
        PaymentRequestDTO request = validRequest();

        when(customerContext.store("tienda-luna")).thenReturn(store);
        when(customerContext.customer(authentication, store)).thenReturn(customer);
        when(orderService.findByCustomerInStore(1, 1, 10)).thenReturn(order);
        when(paymentReceiptService.simulatePayment(
                order, "20123456789", PaymentMethod.VIRTUAL,
                "4111111111111111", "Juan Perez", "12/27", "123"))
                .thenThrow(new BusinessRuleException("Order already has a payment receipt"));

        var result = controller.pay("tienda-luna", 1, authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}