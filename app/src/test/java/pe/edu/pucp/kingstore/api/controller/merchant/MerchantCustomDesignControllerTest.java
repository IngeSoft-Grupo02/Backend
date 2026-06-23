package pe.edu.pucp.kingstore.api.controller.merchant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import pe.edu.pucp.kingstore.api.context.MerchantContext;
import pe.edu.pucp.kingstore.api.controller.MerchantCustomDesignController;
import pe.edu.pucp.kingstore.domain.model.product.CustomDesign;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.product.CustomDesignService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantCustomDesignControllerTest {

    @Mock private MerchantContext      merchantContext;
    @Mock private CustomDesignService  customDesignService;

    private MerchantCustomDesignController controller;
    private Authentication authentication;
    private Store store;

    @BeforeEach
    void setUp() {
        controller     = new MerchantCustomDesignController(merchantContext, customDesignService);
        authentication = mock(Authentication.class);
        store          = new Store();
        store.setId(10);
    }

    private CustomDesign design(Integer id) {
        Product product = new Product();
        product.setId(1);
        product.setStore(store);
        CustomDesign design = new CustomDesign();
        design.setId(id);
        design.setProduct(product);
        design.setDescription("Diseño " + id);
        return design;
    }

    // ── GET /merchant/designs ─────────────────────────────────────────────────

    @Test
    void findAllReturnsDesignsForStore() {
        CustomDesign design = design(1);
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        when(customDesignService.findByStore(10)).thenReturn(List.of(design));

        var result = controller.findAll(authentication, 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<?> body = (List<?>) result.getBody();
        assertThat(body).hasSize(1);
    }

    @Test
    void findAllReturnsBadRequestOnException() {
        when(merchantContext.currentStore(authentication, 10))
                .thenThrow(new BusinessRuleException("Store not found"));

        var result = controller.findAll(authentication, 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── PATCH /merchant/designs/{id}/approve ──────────────────────────────────

    @Test
    void approveReturnsApprovedDesign() {
        CustomDesign design = design(1);
        design.setObservations("APPROVED");
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        when(customDesignService.approve(1, 10)).thenReturn(design);

        var result = controller.approve(authentication, 1, 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void approveReturnsBadRequestWhenAlreadyReviewed() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        when(customDesignService.approve(1, 10))
                .thenThrow(new BusinessRuleException("Design has already been reviewed"));

        var result = controller.approve(authentication, 1, 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── PATCH /merchant/designs/{id}/reject ───────────────────────────────────

    @Test
    void rejectReturnsRejectedDesign() {
        CustomDesign design = design(1);
        design.setObservations("No cumple requisitos");
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        when(customDesignService.reject(1, 10, "No cumple requisitos")).thenReturn(design);

        var result = controller.reject(authentication, 1, 10,
                Map.of("observations", "No cumple requisitos"));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void rejectReturnsBadRequestWhenObservationsBlank() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);

        var result = controller.reject(authentication, 1, 10, Map.of("observations", ""));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectReturnsBadRequestWhenObservationsMissing() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);

        var result = controller.reject(authentication, 1, 10, Map.of());

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}