package pe.edu.pucp.kingstore.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import pe.edu.pucp.kingstore.api.context.MerchantContext;
import pe.edu.pucp.kingstore.domain.dto.quotation.QuotationDesignDTO;
import pe.edu.pucp.kingstore.domain.dto.quotation.QuotationResponseDTO;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.service.order.OrderService;
import pe.edu.pucp.kingstore.service.quotation.QuotationService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantQuotationControllerTest {

    @Mock private MerchantContext merchantContext;
    @Mock private QuotationService quotationService;
    @Mock private OrderService orderService;

    private MerchantQuotationController controller;
    private Authentication authentication;
    private Store store;

    @BeforeEach
    void setUp() {
        controller = new MerchantQuotationController(merchantContext, quotationService, orderService);
        authentication = mock(Authentication.class);
        store = new Store();
        store.setId(10);
    }

    @Test
    void getMerchantQuotations_shouldReturnCustomerDescription() {
        Quotation quotation = new Quotation();
        quotation.setId(1);
        quotation.setStatus(QuotationStatus.PENDING);
        quotation.setDescription("Necesito polos para evento corporativo");

        QuotationResponseDTO dto = new QuotationResponseDTO();
        dto.setId(1);
        dto.setDescription("Necesito polos para evento corporativo");
        dto.setDesigns(List.of(new QuotationDesignDTO(
                1,
                "diseno-frontal.png",
                "https://bucket.s3.amazonaws.com/design/street-kings/quotations/1/diseno-frontal.png",
                "image/png",
                123456L,
                null)));

        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        when(quotationService.findByStoreId(10)).thenReturn(List.of(quotation));
        when(quotationService.toResponseDTO(quotation, 10)).thenReturn(dto);

        var result = controller.quotations(authentication, null, 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<QuotationResponseDTO> body = (List<QuotationResponseDTO>) result.getBody();
        assertThat(body).hasSize(1);
        assertThat(body.get(0).getDescription()).isEqualTo("Necesito polos para evento corporativo");
        assertThat(body.get(0).getDesigns()).hasSize(1);
    }
}
