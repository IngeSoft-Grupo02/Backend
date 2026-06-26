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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReceiptServiceTest {

    @Mock private PaymentReceiptRepository paymentReceiptRepository;
    @Mock private OrderRepository orderRepository;

    private PaymentReceiptService service;

    @BeforeEach
    void setUp() {
        service = new PaymentReceiptService(paymentReceiptRepository, orderRepository);
    }

    @Test
    void confirmPayment_withBoletaReceiptType_shouldSucceedWithoutRuc() {
        Order order = payableOrder();
        when(paymentReceiptRepository.findByOrderId(1)).thenReturn(Optional.empty());
        when(paymentReceiptRepository.save(any(PaymentReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentReceipt receipt = service.simulatePayment(
                order,
                null,
                PaymentMethod.VIRTUAL,
                "BOLETA",
                "4111111111111111",
                "Juan Perez",
                "12/27",
                "123");

        assertThat(receipt.getReceiptType()).isEqualTo(ReceiptType.BOLETA);
        assertThat(receipt.getRuc()).isEqualTo("00000000000");
    }

    @Test
    void confirmPayment_withFacturaReceiptType_shouldSucceedWithValidRuc() {
        Order order = payableOrder();
        when(paymentReceiptRepository.findByOrderId(1)).thenReturn(Optional.empty());
        when(paymentReceiptRepository.save(any(PaymentReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentReceipt receipt = service.simulatePayment(
                order,
                "20123456789",
                PaymentMethod.VIRTUAL,
                "FACTURA",
                "4111111111111111",
                "Juan Perez",
                "12/27",
                "123");

        assertThat(receipt.getReceiptType()).isEqualTo(ReceiptType.FACTURA);
        assertThat(receipt.getRuc()).isEqualTo("20123456789");
    }

    @Test
    void confirmPayment_withoutReceiptType_shouldFailWithClearMessage() {
        Order order = payableOrder();
        when(paymentReceiptRepository.findByOrderId(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.simulatePayment(
                order,
                null,
                PaymentMethod.VIRTUAL,
                null,
                "4111111111111111",
                "Juan Perez",
                "12/27",
                "123"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Receipt type is required (BOLETA or FACTURA)");
    }

    @Test
    void confirmPayment_withInvalidReceiptType_shouldFailWithClearMessage() {
        Order order = payableOrder();
        when(paymentReceiptRepository.findByOrderId(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.simulatePayment(
                order,
                null,
                PaymentMethod.VIRTUAL,
                "TICKET",
                "4111111111111111",
                "Juan Perez",
                "12/27",
                "123"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Receipt type is required (BOLETA or FACTURA)");
    }

    private Order payableOrder() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
        order.setFinalTotal(118.0);
        return order;
    }
}
