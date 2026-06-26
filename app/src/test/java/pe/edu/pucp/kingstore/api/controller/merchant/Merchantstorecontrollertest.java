package pe.edu.pucp.kingstore.api.controller.merchant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import pe.edu.pucp.kingstore.api.context.MerchantContext;
import pe.edu.pucp.kingstore.api.controller.MerchantStoreController;
import pe.edu.pucp.kingstore.domain.dto.store.DashboardResponse;
import pe.edu.pucp.kingstore.domain.dto.store.MerchantStoreRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.store.StoreCategoryResponse;
import pe.edu.pucp.kingstore.domain.dto.store.StoreResponseDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.StoreCategory;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.storage.StorageService;
import pe.edu.pucp.kingstore.service.store.DashboardService;
import pe.edu.pucp.kingstore.service.store.StoreCategoryService;
import pe.edu.pucp.kingstore.service.store.StoreService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre todos los endpoints de MerchantStoreController:
 *  - GET /merchant/stores
 *  - GET /merchant/store
 *  - GET /merchant/categories (sin y con search)
 *  - POST /merchant/stores/logo (validaciones y caso exitoso)
 *  - POST /merchant/stores
 *  - PUT /merchant/stores/{id} JSON y multipart
 *  - DELETE /merchant/stores/{id}
 *  - GET /merchant/dashboard
 *  - manejo genérico de errores vía handle()
 */
@ExtendWith(MockitoExtension.class)
class MerchantStoreControllerTest {

    @Mock private MerchantContext merchantContext;
    @Mock private StoreService storeService;
    @Mock private StoreCategoryService storeCategoryService;
    @Mock private StorageService storageService;
    @Mock private DashboardService dashboardService;

    private MerchantStoreController controller;
    private Authentication authentication;
    private Store store;

    @BeforeEach
    void setUp() {
        controller = new MerchantStoreController(
                merchantContext, storeService, storeCategoryService, storageService, dashboardService);
        authentication = mock(Authentication.class);
        store = new Store();
        store.setId(10);
        store.setSlug("mi-tienda");
    }

    private StoreResponseDTO responseDTO(int id) {
        return new StoreResponseDTO(id, "Mi Tienda", "mi-tienda", "desc", "logo.png",
                "ACTIVE", "ONYX_BLACK", "SLATE", "RAW_GOLD", 1, "Ropa", 0L);
    }

    private StoreCategory category(int id, String name) {
        StoreCategory cat = new StoreCategory();
        cat.setId(id);
        cat.setStoreCategoryName(name);
        return cat;
    }

    // =========================================================================
    // GET /merchant/stores
    // =========================================================================

    @Test
    void storesReturnsAllMerchantStores() {
        when(merchantContext.stores(authentication)).thenReturn(List.of(store));
        when(storeService.toResponseDTO(store)).thenReturn(responseDTO(10));

        var result = controller.stores(authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<StoreResponseDTO> body = (List<StoreResponseDTO>) result.getBody();
        assertThat(body).hasSize(1);
    }

    // =========================================================================
    // GET /merchant/store
    // =========================================================================

    @Test
    void currentStoreReturnsResolvedStore() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        when(storeService.toResponseDTO(store)).thenReturn(responseDTO(10));

        var result = controller.currentStore(authentication, 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((StoreResponseDTO) result.getBody()).getId()).isEqualTo(10);
    }

    // =========================================================================
    // GET /merchant/categories
    // =========================================================================

    @Test
    void categoriesReturnsAllWhenSearchNull() {
        when(storeCategoryService.findActive()).thenReturn(List.of(category(1, "Ropa"), category(2, "Calzado")));

        var result = controller.categories(null);

        @SuppressWarnings("unchecked")
        List<StoreCategoryResponse> body = (List<StoreCategoryResponse>) result.getBody();
        assertThat(body).hasSize(2);
    }

    @Test
    void categoriesFiltersBySearchTerm() {
        when(storeCategoryService.findActive()).thenReturn(List.of(category(1, "Ropa"), category(2, "Calzado")));

        var result = controller.categories("rop");

        @SuppressWarnings("unchecked")
        List<StoreCategoryResponse> body = (List<StoreCategoryResponse>) result.getBody();
        assertThat(body).hasSize(1);
        assertThat(body.get(0).name()).isEqualTo("Ropa");
    }

    @Test
    void categoriesReturnsAllWhenSearchBlank() {
        when(storeCategoryService.findActive()).thenReturn(List.of(category(1, "Ropa")));

        var result = controller.categories("   ");

        @SuppressWarnings("unchecked")
        List<StoreCategoryResponse> body = (List<StoreCategoryResponse>) result.getBody();
        assertThat(body).hasSize(1);
    }

    // =========================================================================
    // POST /merchant/stores/logo
    // =========================================================================

    @Test
    void uploadStoreLogoThrowsWhenLogoEmpty() {
        Merchant merchant = new Merchant();
        merchant.setId(1);
        when(merchantContext.merchant(authentication)).thenReturn(merchant);
        MockMultipartFile empty = new MockMultipartFile("logo", "logo.png", "image/png", new byte[0]);

        var result = controller.uploadStoreLogo(authentication, empty);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploadStoreLogoThrowsWhenExtensionInvalid() {
        Merchant merchant = new Merchant();
        merchant.setId(1);
        when(merchantContext.merchant(authentication)).thenReturn(merchant);
        MockMultipartFile file = new MockMultipartFile("logo", "logo.pdf", "application/pdf", "data".getBytes());

        var result = controller.uploadStoreLogo(authentication, file);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploadStoreLogoThrowsWhenTooLarge() {
        Merchant merchant = new Merchant();
        merchant.setId(1);
        when(merchantContext.merchant(authentication)).thenReturn(merchant);
        byte[] tooBig = new byte[(int) (2L * 1024 * 1024 + 1)];
        MockMultipartFile file = new MockMultipartFile("logo", "logo.png", "image/png", tooBig);

        var result = controller.uploadStoreLogo(authentication, file);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploadStoreLogoSucceedsForValidImage() {
        Merchant merchant = new Merchant();
        merchant.setId(1);
        when(merchantContext.merchant(authentication)).thenReturn(merchant);
        MockMultipartFile file = new MockMultipartFile("logo", "logo.png", "image/png", "data".getBytes());
        when(storageService.uploadBytes(anyString(), any(), anyString()))
                .thenReturn("https://bucket.s3.amazonaws.com/logos/uuid-logo.png");

        var result = controller.uploadStoreLogo(authentication, file);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) result.getBody();
        assertThat(body.get("logoUrl")).isEqualTo("https://bucket.s3.amazonaws.com/logos/uuid-logo.png");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService).uploadBytes(keyCaptor.capture(), any(), anyString());
        assertThat(keyCaptor.getValue()).startsWith("logos/");
        assertThat(keyCaptor.getValue()).endsWith("-logo.png");
    }

    // =========================================================================
    // POST /merchant/stores
    // =========================================================================

    @Test
    void createStoreReturns201() {
        Merchant merchant = new Merchant();
        merchant.setId(1);
        when(merchantContext.merchant(authentication)).thenReturn(merchant);

        MerchantStoreRequestDTO request = new MerchantStoreRequestDTO();
        when(storeService.createForMerchant(merchant, request)).thenReturn(store);
        when(storeService.toResponseDTO(store)).thenReturn(responseDTO(10));

        var result = controller.createStore(authentication, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(((StoreResponseDTO) result.getBody()).getId()).isEqualTo(10);
    }

    // =========================================================================
    // PUT /merchant/stores/{id}
    // =========================================================================

    @Test
    void updateStoreReturnsUpdatedStore() {
        when(merchantContext.storeById(authentication, 10)).thenReturn(store);
        MerchantStoreRequestDTO request = new MerchantStoreRequestDTO();
        when(storeService.updateForMerchant(store, request)).thenReturn(store);
        when(storeService.toResponseDTO(store)).thenReturn(responseDTO(10));

        var result = controller.updateStore(authentication, 10, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(storeService).updateForMerchant(store, request);
    }

    @Test
    void updateStoreWithLogoUploadsAndUpdatesStoreOnce() {
        when(merchantContext.storeById(authentication, 10)).thenReturn(store);
        MerchantStoreRequestDTO request = new MerchantStoreRequestDTO();
        request.setName("Street Kings");
        MockMultipartFile file = new MockMultipartFile("logo", "logo nuevo.png", "image/png", "data".getBytes());
        when(storageService.uploadBytes(anyString(), any(), anyString()))
                .thenReturn("https://bucket.s3.amazonaws.com/logos/street-kings.png");
        when(storeService.updateForMerchant(store, request)).thenReturn(store);
        when(storeService.toResponseDTO(store)).thenReturn(responseDTO(10));

        var result = controller.updateStoreWithLogo(authentication, 10, request, file);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(request.getLogoUrl()).isEqualTo("https://bucket.s3.amazonaws.com/logos/street-kings.png");
        verify(storeService).updateForMerchant(store, request);

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(storageService).uploadBytes(keyCaptor.capture(), any(), anyString());
        assertThat(keyCaptor.getValue()).isEqualTo("logos/street-kings.png");
    }

    @Test
    void updateStoreWithLogoRejectsInvalidExtension() {
        when(merchantContext.storeById(authentication, 10)).thenReturn(store);
        MerchantStoreRequestDTO request = new MerchantStoreRequestDTO();
        MockMultipartFile file = new MockMultipartFile("logo", "logo.pdf", "application/pdf", "data".getBytes());

        var result = controller.updateStoreWithLogo(authentication, 10, request, file);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateStoreWithLogoRejectsTooLargeFile() {
        when(merchantContext.storeById(authentication, 10)).thenReturn(store);
        MerchantStoreRequestDTO request = new MerchantStoreRequestDTO();
        byte[] tooBig = new byte[(int) (2L * 1024 * 1024 + 1)];
        MockMultipartFile file = new MockMultipartFile("logo", "logo.png", "image/png", tooBig);

        var result = controller.updateStoreWithLogo(authentication, 10, request, file);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // =========================================================================
    // DELETE /merchant/stores/{id}
    // =========================================================================

    @Test
    void deleteStoreDeactivatesAndReturnsMessage() {
        when(merchantContext.storeById(authentication, 10)).thenReturn(store);

        var result = controller.deleteStore(authentication, 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) result.getBody();
        assertThat(body.get("message")).isEqualTo("Store deactivated successfully");
        verify(storeService).deactivate(10);
    }

    // =========================================================================
    // GET /merchant/dashboard
    // =========================================================================

    @Test
    void dashboardReturnsDashboardData() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        DashboardResponse dashboard = new DashboardResponse(3, 2, 1, List.of());
        when(dashboardService.getDashboardData(10)).thenReturn(dashboard);

        var result = controller.dashboard(authentication, 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((DashboardResponse) result.getBody()).pendingOrders()).isEqualTo(3);
    }

    // =========================================================================
    // Manejo de errores genérico (vía handle())
    // =========================================================================

    @Test
    void currentStoreReturnsBadRequestWhenContextThrowsBusinessRule() {
        when(merchantContext.currentStore(authentication, null))
                .thenThrow(new BusinessRuleException("Store context is required"));

        var result = controller.currentStore(authentication, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
