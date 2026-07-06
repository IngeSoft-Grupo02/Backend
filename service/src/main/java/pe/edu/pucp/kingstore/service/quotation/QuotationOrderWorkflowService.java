package pe.edu.pucp.kingstore.service.quotation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.service.order.OrderService;

@Service
public class QuotationOrderWorkflowService {

    private final QuotationService quotationService;
    private final OrderService orderService;

    public QuotationOrderWorkflowService(QuotationService quotationService,
                                         OrderService orderService) {
        this.quotationService = quotationService;
        this.orderService = orderService;
    }

    @Transactional
    public Quotation respondMerchantQuotation(Integer quotationId,
                                              Integer storeId,
                                              QuotationStatus status,
                                              String observations,
                                              Double discountAmount) {
        Quotation quotation = quotationService.findInStore(quotationId, storeId);
        Quotation responded = quotationService.respond(
                quotation.getId(),
                status,
                observations,
                discountAmount);

        if (responded.getStatus() == QuotationStatus.APPROVED) {
            orderService.createFromQuotation(responded.getId());
        }
        return responded;
    }

    @Transactional
    public Order ensureCustomerOrderFromApprovedQuotation(Integer quotationId,
                                                          Integer customerId,
                                                          Integer storeId) {
        Quotation quotation = quotationService.findByCustomerInStore(
                quotationId, customerId, storeId);
        return orderService.createFromQuotation(quotation);
    }
}
