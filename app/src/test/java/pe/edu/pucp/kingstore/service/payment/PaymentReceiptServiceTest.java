package pe.edu.pucp.kingstore.service.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;
import pe.edu.pucp.kingstore.domain.model.payment.PaymentReceipt;
import pe.edu.pucp.kingstore.domain.model.payment.enums.PaymentMethod;
import pe.edu.pucp.kingstore.domain.model.payment.enums.ReceiptType;
import pe.edu.pucp.kingstore.repository.order.OrderRepository;
import pe.edu.pucp.kingstore.repository.payment.PaymentReceiptRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

import java.time.YearMonth;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReceiptServiceTest {

    @Mock
    private PaymentReceiptRepository paymentReceiptRepository;
    @Mock
    private OrderRepository orderRepository;

    private PaymentReceiptService service;

    @BeforeEach
    void setUp() {
        service = new PaymentReceiptService(paymentReceiptRepository, orderRepository);
    }

    @Test
    void simulatePaymentCreatesBoletaWithDefaultValues() {
        Order order = order(OrderStatus.PAYMENT_CONFIRMED);
        when(paymentReceiptRepository.findByOrderId(77)).thenReturn(Optional.empty());
        when(paymentReceiptRepository.save(any(PaymentReceipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentReceipt receipt = service.simulatePayment(
                order,
                null,
                null,
                "BOLETA",
                "4111 1111 1111 1111",
                "Ana Perez",
                futureExpiry(),
                "123"
        );

        assertThat(receipt.getOrder()).isSameAs(order);
        assertThat(receipt.getReceiptType()).isEqualTo(ReceiptType.BOLETA);
        assertThat(receipt.getRuc()).isEqualTo("00000000000");
        assertThat(receipt.getPaymentMethod()).isEqualTo(PaymentMethod.VIRTUAL);
        assertThat(receipt.getFinalTotal()).isEqualTo(118.0);
        assertThat(receipt.getTaxes()).isGreaterThan(0);
        verify(orderRepository).save(order);
    }

    @Test
    void simulatePaymentCreatesFacturaWhenRucIsValid() {
        Order order = order(OrderStatus.PAYMENT_CONFIRMED);
        when(paymentReceiptRepository.findByOrderId(77)).thenReturn(Optional.empty());
        when(paymentReceiptRepository.save(any(PaymentReceipt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentReceipt receipt = service.simulatePayment(
                order,
                "20123456789",
                PaymentMethod.CASH_ON_DELIVERY,
                "FACTURA",
                "4111111111111111",
                "Ana Perez",
                futureExpiry(),
                "123"
        );

        assertThat(receipt.getReceiptType()).isEqualTo(ReceiptType.FACTURA);
        assertThat(receipt.getRuc()).isEqualTo("20123456789");
        assertThat(receipt.getPaymentMethod()).isEqualTo(PaymentMethod.CASH_ON_DELIVERY);
        assertThat(receipt.getSubTotal()).isEqualTo(118.0);
    }

    @Test
    void simulatePaymentRejectsMissingOrInvalidReceiptType() {
        Order order = order(OrderStatus.PAYMENT_CONFIRMED);
        when(paymentReceiptRepository.findByOrderId(77)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.simulatePayment(
                order, null, PaymentMethod.VIRTUAL, null, "4111111111111111", "Ana Perez", futureExpiry(), "123"
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Receipt type is required (BOLETA or FACTURA)");

        assertThatThrownBy(() -> service.simulatePayment(
                order, null, PaymentMethod.VIRTUAL, "TICKET", "4111111111111111", "Ana Perez", futureExpiry(), "123"
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Receipt type is required (BOLETA or FACTURA)");
    }

    @Test
    void simulatePaymentRejectsInvalidOrderAndExistingReceipt() {
        Order pending = order(OrderStatus.IN_PREPARATION);

        assertThatThrownBy(() -> service.simulatePayment(
                pending, null, null, "BOLETA", "4111111111111111", "Ana Perez", futureExpiry(), "123"
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Only confirmed orders can be paid");

        Order confirmed = order(OrderStatus.PAYMENT_CONFIRMED);
        when(paymentReceiptRepository.findByOrderId(77)).thenReturn(Optional.of(new PaymentReceipt()));

        assertThatThrownBy(() -> service.simulatePayment(
                confirmed, null, null, "BOLETA", "4111111111111111", "Ana Perez", futureExpiry(), "123"
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Order already has a payment receipt");
    }

    @Test
    void simulatePaymentRejectsCardValidationFailures() {
        Order order = order(OrderStatus.PAYMENT_CONFIRMED);
        when(paymentReceiptRepository.findByOrderId(77)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.simulatePayment(
                order, null, null, "BOLETA", "4111", "Ana Perez", futureExpiry(), "123"
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Card number must have 16 digits");

        assertThatThrownBy(() -> service.simulatePayment(
                order, null, null, "BOLETA", "411111111111111A", "Ana Perez", futureExpiry(), "123"
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Card number must contain only digits");

        assertThatThrownBy(() -> service.simulatePayment(
                order, null, null, "BOLETA", "4111111111111111", " ", futureExpiry(), "123"
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Card holder name is required");

        assertThatThrownBy(() -> service.simulatePayment(
                order, null, null, "BOLETA", "4111111111111111", "Ana Perez", futureExpiry(), "12"
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessage("CVV must have 3 digits");
    }

    @Test
    void simulatePaymentRejectsExpiryDeclinedCardAndInvalidRuc() {
        Order order = order(OrderStatus.PAYMENT_CONFIRMED);
        when(paymentReceiptRepository.findByOrderId(77)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.simulatePayment(
                order, null, null, "BOLETA", "4111111111111111", "Ana Perez", "13/30", "123"
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Expiry date must be in MM/YY format");

        assertThatThrownBy(() -> service.simulatePayment(
                order, null, null, "BOLETA", "4111111111111111", "Ana Perez", "01/20", "123"
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Card has expired");

        assertThatThrownBy(() -> service.simulatePayment(
                order, null, null, "BOLETA", "4111111111110000", "Ana Perez", futureExpiry(), "123"
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessage("Payment declined — card rejected by issuer");

        assertThatThrownBy(() -> service.simulatePayment(
                order, null, PaymentMethod.VIRTUAL, "FACTURA", "4111111111111111", "Ana Perez", futureExpiry(), "123"
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessage("RUC is required for invoices");

        assertThatThrownBy(() -> service.simulatePayment(
                order, "123", PaymentMethod.VIRTUAL, "FACTURA", "4111111111111111", "Ana Perez", futureExpiry(), "123"
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessage("RUC must have 11 digits");

        assertThatThrownBy(() -> service.simulatePayment(
                order, "15123456789", PaymentMethod.VIRTUAL, "FACTURA", "4111111111111111", "Ana Perez", futureExpiry(), "123"
        )).isInstanceOf(BusinessRuleException.class)
                .hasMessage("RUC must start with 10 or 20");
    }

    @Test
    void createValidatesRequiredReceiptFields() {
        PaymentReceipt receipt = new PaymentReceipt();
        assertThatThrownBy(() -> service.create(receipt))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Payment receipt must belong to an order");

        receipt.setOrder(order(OrderStatus.PAYMENT_CONFIRMED));
        assertThatThrownBy(() -> service.create(receipt))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("RUC");

        receipt.setRuc("20123456789");
        assertThatThrownBy(() -> service.create(receipt))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Payment method is required");
    }

    private Order order(OrderStatus status) {
        Order order = new Order();
        order.setId(77);
        order.setStatus(status);
        order.setFinalTotal(118.0);
        return order;
    }

    private String futureExpiry() {
        YearMonth future = YearMonth.now().plusYears(1);
        return "%02d/%02d".formatted(future.getMonthValue(), future.getYear() % 100);
    }
}
