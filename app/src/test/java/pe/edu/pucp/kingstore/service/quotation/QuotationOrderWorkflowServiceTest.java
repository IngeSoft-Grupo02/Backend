package pe.edu.pucp.kingstore.service.quotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.service.order.OrderService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuotationOrderWorkflowServiceTest {

    @Mock private QuotationService quotationService;
    @Mock private OrderService orderService;

    private QuotationOrderWorkflowService service;

    @BeforeEach
    void setUp() {
        service = new QuotationOrderWorkflowService(quotationService, orderService);
    }

    @Test
    void respondMerchantQuotationApprovedEnsuresOrder() {
        Quotation scoped = quotation(10, QuotationStatus.PENDING);
        Quotation approved = quotation(10, QuotationStatus.APPROVED);

        when(quotationService.findInStore(10, 5)).thenReturn(scoped);
        when(quotationService.respond(10, QuotationStatus.APPROVED, "ok", 2.0))
                .thenReturn(approved);

        Quotation result = service.respondMerchantQuotation(
                10, 5, QuotationStatus.APPROVED, "ok", 2.0);

        assertThat(result).isSameAs(approved);
        verify(orderService).createFromQuotation(10);
    }

    @Test
    void respondMerchantQuotationRejectedDoesNotCreateOrder() {
        Quotation scoped = quotation(11, QuotationStatus.PENDING);
        Quotation rejected = quotation(11, QuotationStatus.REJECTED);

        when(quotationService.findInStore(11, 5)).thenReturn(scoped);
        when(quotationService.respond(11, QuotationStatus.REJECTED, "Sin stock", null))
                .thenReturn(rejected);

        Quotation result = service.respondMerchantQuotation(
                11, 5, QuotationStatus.REJECTED, "Sin stock", null);

        assertThat(result).isSameAs(rejected);
        verify(orderService, never()).createFromQuotation(11);
    }

    @Test
    void ensureCustomerOrderFromApprovedQuotationDelegatesToOrderService() {
        Quotation quotation = quotation(12, QuotationStatus.APPROVED);
        Order order = new Order();
        order.setId(99);

        when(quotationService.findByCustomerInStore(12, 3, 5)).thenReturn(quotation);
        when(orderService.createFromQuotation(quotation)).thenReturn(order);

        Order result = service.ensureCustomerOrderFromApprovedQuotation(12, 3, 5);

        assertThat(result).isSameAs(order);
    }

    private Quotation quotation(Integer id, QuotationStatus status) {
        Quotation quotation = new Quotation();
        quotation.setId(id);
        quotation.setStatus(status);
        return quotation;
    }
}
