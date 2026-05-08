package pe.edu.pucp.kingstore.service.payment;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.payment.PaymentReceipt;
import pe.edu.pucp.kingstore.repository.payment.PaymentReceiptRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

import java.util.Optional;

@Service
public class PaymentReceiptService extends AbstractCrudService<PaymentReceipt> {

    private static final double IGV_RATE = 0.18;

    private final PaymentReceiptRepository paymentReceiptRepository;

    public PaymentReceiptService(PaymentReceiptRepository paymentReceiptRepository) {
        super(paymentReceiptRepository, "Payment receipt");
        this.paymentReceiptRepository = paymentReceiptRepository;
    }

    @Transactional(readOnly = true)
    public Optional<PaymentReceipt> findByOrder(Integer orderId) {
        requireId(orderId);
        return paymentReceiptRepository.findByOrderId(orderId);
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
