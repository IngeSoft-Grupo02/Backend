package pe.edu.pucp.kingstore.api.controller.merchant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import pe.edu.pucp.kingstore.api.context.MerchantContext;
import pe.edu.pucp.kingstore.domain.dto.quotation.QuotationItemResponseDTO;
import pe.edu.pucp.kingstore.domain.dto.quotation.QuotationResponseDTO;
import pe.edu.pucp.kingstore.domain.dto.quotation.QuotationResponseRequestDTO;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.quotation.QuotationService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre los endpoints de MerchantQuotationController:
 *  - GET /merchant/quotations (sin filtro, con filtro de status válido e inválido)
 *  - PATCH /merchant/quotations/{id}/respond (caso exitoso, no encontrado y regla de negocio)
 */
@ExtendWith(MockitoExtension.class)
class MerchantQuotationControllerTest {

    @Mock
    private MerchantContext merchantContext;

    @Mock
    private QuotationService quotationService;

    private MerchantQuotationController controller;
    private Authentication authentication;
    private Store store;

    @BeforeEach
    void setUp() {
        controller = new MerchantQuotationController(merchantContext, quotationService);
        authentication = mock(Authentication.class);
        store = new Store();
        store.setId(10);
    }

    private QuotationResponseDTO responseDTO(Integer id, QuotationStatus status) {
        return new QuotationResponseDTO(
                id, "Cliente", status, "Pendiente",
                100.0, 0.0, 100.0, null, "desc", null, store.getId(),
                List.of(new QuotationItemResponseDTO("Producto", "M / BLACK", 1, 100.0, 100.0))
        );
    }

    // =========================================================================
    // GET /merchant/quotations
    // =========================================================================

    @Test
    void quotationsReturnsAllWhenStatusNotProvided() {
        when(merchantContext.currentStore(authentication, null)).thenReturn(store);
        Quotation quotation = new Quotation();
        quotation.setId(1);
        when(quotationService.findByStoreId(10)).thenReturn(List.of(quotation));
        when(quotationService.toResponseDTO(quotation, 10)).thenReturn(responseDTO(1, QuotationStatus.PENDING));

        var result = controller.quotations(authentication, null, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<QuotationResponseDTO> body = (List<QuotationResponseDTO>) result.getBody();
        assertThat(body).hasSize(1);
        assertThat(body.get(0).getId()).isEqualTo(1);
    }

    @Test
    void quotationsReturnsAllWhenStatusBlank() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        when(quotationService.findByStoreId(10)).thenReturn(List.of());

        var result = controller.quotations(authentication, "  ", 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(quotationService).findByStoreId(10);
    }

    @Test
    void quotationsFiltersByStatusWhenProvided() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        Quotation quotation = new Quotation();
        quotation.setId(2);
        when(quotationService.findByStoreIdAndStatus(10, QuotationStatus.APPROVED))
                .thenReturn(List.of(quotation));
        when(quotationService.toResponseDTO(quotation, 10)).thenReturn(responseDTO(2, QuotationStatus.APPROVED));

        var result = controller.quotations(authentication, "aprobada", 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(quotationService).findByStoreIdAndStatus(10, QuotationStatus.APPROVED);
    }

    @Test
    void quotationsReturnsBadRequestWhenStatusInvalid() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);

        var result = controller.quotations(authentication, "no-existe", 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // =========================================================================
    // PATCH /merchant/quotations/{id}/respond
    // =========================================================================

    @Test
    void respondQuotationApprovesSuccessfully() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);

        Quotation found = new Quotation();
        found.setId(5);
        when(quotationService.findInStore(5, 10)).thenReturn(found);

        Quotation responded = new Quotation();
        responded.setId(5);
        responded.setStatus(QuotationStatus.APPROVED);
        when(quotationService.respond(5, QuotationStatus.APPROVED, "Listo")).thenReturn(responded);
        when(quotationService.toResponseDTO(responded, 10)).thenReturn(responseDTO(5, QuotationStatus.APPROVED));

        QuotationResponseRequestDTO request = new QuotationResponseRequestDTO();
        request.setStatus(QuotationStatus.APPROVED);
        request.setObservations("Listo");

        var result = controller.respondQuotation(authentication, 5, 10, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        QuotationResponseDTO body = (QuotationResponseDTO) result.getBody();
        assertThat(body.getId()).isEqualTo(5);
        assertThat(body.getStatus()).isEqualTo(QuotationStatus.APPROVED);
    }

    @Test
    void respondQuotationReturnsNotFoundWhenQuotationDoesNotBelongToStore() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        when(quotationService.findInStore(anyInt(), anyInt()))
                .thenThrow(new ResourceNotFoundException("Quotation", 99));

        QuotationResponseRequestDTO request = new QuotationResponseRequestDTO();
        request.setStatus(QuotationStatus.APPROVED);

        var result = controller.respondQuotation(authentication, 99, 10, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void respondQuotationReturnsBadRequestWhenBusinessRuleFails() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);

        Quotation found = new Quotation();
        found.setId(6);
        when(quotationService.findInStore(6, 10)).thenReturn(found);
        when(quotationService.respond(anyInt(), any(), any()))
                .thenThrow(new BusinessRuleException("Quotation response must approve or reject the quotation"));

        QuotationResponseRequestDTO request = new QuotationResponseRequestDTO();
        request.setStatus(QuotationStatus.PENDING);

        var result = controller.respondQuotation(authentication, 6, 10, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}