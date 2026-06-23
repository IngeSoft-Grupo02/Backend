package pe.edu.pucp.kingstore.service.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.payment.PaymentReceipt;
import pe.edu.pucp.kingstore.repository.payment.PaymentReceiptRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;
import pe.edu.pucp.kingstore.repository.order.OrderRepository;
import java.util.Optional;

@Service
public class PaymentReceiptService extends AbstractCrudService<PaymentReceipt> {

    private static final double IGV_RATE = 0.18;

    private final PaymentReceiptRepository paymentReceiptRepository;
    private final OrderRepository          orderRepository;
    public PaymentReceiptService(PaymentReceiptRepository paymentReceiptRepository, OrderRepository orderRepository) {
        super(paymentReceiptRepository, "Payment receipt");
        this.paymentReceiptRepository = paymentReceiptRepository;
        this.orderRepository          = orderRepository;
    }

    @Transactional(readOnly = true)
    public Optional<PaymentReceipt> findByOrder(Integer orderId) {
        requireId(orderId);
        return paymentReceiptRepository.findByOrderId(orderId);
    }

    /*/**
     * Pago simulado con tarjeta.
     * Valida los datos de tarjeta pero NO los persiste.
     * Si el número termina en 0000 → simula tarjeta declinada.
     */
    @Transactional
    public PaymentReceipt simulatePayment(Order order, String ruc,
                                          pe.edu.pucp.kingstore.domain.model.payment.enums.PaymentMethod paymentMethod,
                                          String cardNumber, String cardHolder,
                                          String expiryDate, String cvv) {
        if (order.getStatus() != OrderStatus.PAYMENT_CONFIRMED) {
            throw new pe.edu.pucp.kingstore.service.common.BusinessRuleException(
                    "Only confirmed orders can be paid");
        }
        paymentReceiptRepository.findByOrderId(order.getId()).ifPresent(existing -> {
            throw new pe.edu.pucp.kingstore.service.common.BusinessRuleException(
                    "Order already has a payment receipt");
        });

        // Validar campos de tarjeta
        validateCard(cardNumber, cardHolder, expiryDate, cvv);

        // Simular tarjeta declinada
        if (cardNumber != null && cardNumber.replaceAll("\\s", "").endsWith("0000")) {
            throw new pe.edu.pucp.kingstore.service.common.BusinessRuleException(
                    "Payment declined — card rejected by issuer");
        }

        PaymentReceipt receipt = new PaymentReceipt();
        receipt.setOrder(order);
        receipt.setRuc(ruc != null ? ruc : "00000000000");
        receipt.setPaymentMethod(paymentMethod != null ? paymentMethod
                : pe.edu.pucp.kingstore.domain.model.payment.enums.PaymentMethod.VIRTUAL);
        receipt.setFinalTotal(order.getFinalTotal());

        order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
        orderRepository.save(order);

        return create(receipt);
    }

    private void validateCard(String cardNumber, String cardHolder,
                              String expiryDate, String cvv) {
        if (cardNumber == null || cardNumber.replaceAll("\\s", "").length() != 16) {
            throw new pe.edu.pucp.kingstore.service.common.BusinessRuleException(
                    "Card number must have 16 digits");
        }
        if (!cardNumber.replaceAll("\\s", "").matches("\\d{16}")) {
            throw new pe.edu.pucp.kingstore.service.common.BusinessRuleException(
                    "Card number must contain only digits");
        }
        if (cardHolder == null || cardHolder.isBlank()) {
            throw new pe.edu.pucp.kingstore.service.common.BusinessRuleException(
                    "Card holder name is required");
        }
        if (cvv == null || !cvv.matches("\\d{3}")) {
            throw new pe.edu.pucp.kingstore.service.common.BusinessRuleException(
                    "CVV must have 3 digits");
        }
        if (expiryDate == null || !expiryDate.matches("(0[1-9]|1[0-2])/\\d{2}")) {
            throw new pe.edu.pucp.kingstore.service.common.BusinessRuleException(
                    "Expiry date must be in MM/YY format");
        }
        // Validar que no sea fecha pasada
        String[] parts = expiryDate.split("/");
        int month = Integer.parseInt(parts[0]);
        int year  = Integer.parseInt(parts[1]) + 2000;
        java.time.YearMonth expiry  = java.time.YearMonth.of(year, month);
        java.time.YearMonth current = java.time.YearMonth.now();
        if (expiry.isBefore(current)) {
            throw new pe.edu.pucp.kingstore.service.common.BusinessRuleException(
                    "Card has expired");
        }
    }

    @Override
    protected void validateForSave(PaymentReceipt receipt) {
        if (receipt.getOrder() == null || receipt.getOrder().getId() == null) {
            throw new BusinessRuleException("Payment receipt must belong to an order");
        }
        requireText(receipt.getRuc(), "RUC");
        if (receipt.getPaymentMethod() == null) {
            throw new BusinessRuleException("Payment method is required");
        }
        recalculateTotals(receipt);
    }

    private void recalculateTotals(PaymentReceipt receipt) {
        Double finalTotal = receipt.getFinalTotal();
        if (finalTotal == null || finalTotal < 0) {
            throw new BusinessRuleException("Payment receipt final total cannot be negative");
        }
        double totalWithoutTax = finalTotal / (1 + IGV_RATE);
        double taxes = finalTotal - totalWithoutTax;

        receipt.setTotalWithoutTax(totalWithoutTax);
        receipt.setTaxes(taxes);
        receipt.setSubTotal(finalTotal);
    }
}
