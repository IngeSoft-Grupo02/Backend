package pe.edu.pucp.kingstore.api.controller.merchant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import pe.edu.pucp.kingstore.api.context.MerchantContext;
import pe.edu.pucp.kingstore.api.controller.MerchantProductController;
import pe.edu.pucp.kingstore.domain.dto.product.ProductRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.product.ProductResponseDTO;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.service.product.ProductService;
import pe.edu.pucp.kingstore.service.storage.StorageService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre todos los endpoints de MerchantProductController:
 *  - GET /merchant/products (sin filtros, con active, con search, con ambos)
 *  - GET /merchant/products/{id}
 *  - POST /merchant/products
 *  - PUT /merchant/products/{id}
 *  - PATCH /merchant/products/{id}/active (válido e inválido)
 *  - DELETE /merchant/products/{id}
 *  - POST /merchant/products/images (validaciones de imagen y caso exitoso)
 */
@ExtendWith(MockitoExtension.class)
class MerchantProductControllerTest {

    @Mock
    private MerchantContext merchantContext;

    @Mock
    private ProductService productService;

    @Mock
    private StorageService storageService;

    private MerchantProductController controller;
    private Authentication authentication;
    private Store store;

    @BeforeEach
    void setUp() {
        controller = new MerchantProductController(merchantContext, productService, storageService);
        authentication = mock(Authentication.class);
        store = new Store();
        store.setId(10);
    }

    private Product product(int id, String name, Boolean active) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setActive(active);
        product.setStore(store);
        return product;
    }

    private ProductResponseDTO responseDTO(int id) {
        return new ProductResponseDTO(id, "Polo", "desc", 50.0, 20.0,
                List.of(), true, "Activo", 5, List.of(), store.getId());
    }

    // =========================================================================
    // GET /merchant/products
    // =========================================================================

    @Test
    void productsReturnsAllWhenNoFilters() {
        when(merchantContext.currentStore(authentication, null)).thenReturn(store);
        Product p1 = product(1, "Polo Azul", true);
        Product p2 = product(2, "Casaca Negra", false);
        when(productService.findByStore(10)).thenReturn(List.of(p1, p2));
        when(productService.toResponseDTO(p1)).thenReturn(responseDTO(1));
        when(productService.toResponseDTO(p2)).thenReturn(responseDTO(2));

        var result = controller.products(authentication, null, null, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<ProductResponseDTO> body = (List<ProductResponseDTO>) result.getBody();
        assertThat(body).hasSize(2);
    }

    @Test
    void productsFiltersByActive() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        Product active = product(1, "Polo Azul", true);
        Product inactive = product(2, "Casaca Negra", false);
        when(productService.findByStore(10)).thenReturn(List.of(active, inactive));
        when(productService.toResponseDTO(active)).thenReturn(responseDTO(1));

        var result = controller.products(authentication, null, true, 10);

        @SuppressWarnings("unchecked")
        List<ProductResponseDTO> body = (List<ProductResponseDTO>) result.getBody();
        assertThat(body).hasSize(1);
    }

    @Test
    void productsFiltersBySearchTerm() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        Product matching = product(1, "Polo Azul", true);
        Product notMatching = product(2, "Casaca Negra", true);
        when(productService.findByStore(10)).thenReturn(List.of(matching, notMatching));
        when(productService.toResponseDTO(matching)).thenReturn(responseDTO(1));

        var result = controller.products(authentication, "polo", null, 10);

        @SuppressWarnings("unchecked")
        List<ProductResponseDTO> body = (List<ProductResponseDTO>) result.getBody();
        assertThat(body).hasSize(1);
    }

    @Test
    void productsCombinesActiveAndSearchFilters() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        Product matchesBoth = product(1, "Polo Azul", true);
        Product wrongName = product(2, "Casaca Negra", true);
        Product wrongActive = product(3, "Polo Rojo", false);
        when(productService.findByStore(10)).thenReturn(List.of(matchesBoth, wrongName, wrongActive));
        when(productService.toResponseDTO(matchesBoth)).thenReturn(responseDTO(1));

        var result = controller.products(authentication, "polo", true, 10);

        @SuppressWarnings("unchecked")
        List<ProductResponseDTO> body = (List<ProductResponseDTO>) result.getBody();
        assertThat(body).hasSize(1);
    }

    @Test
    void productsHandlesProductWithNullName() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        Product noName = product(1, null, true);
        when(productService.findByStore(10)).thenReturn(List.of(noName));

        var result = controller.products(authentication, "polo", null, 10);

        @SuppressWarnings("unchecked")
        List<ProductResponseDTO> body = (List<ProductResponseDTO>) result.getBody();
        assertThat(body).isEmpty();
    }

    // =========================================================================
    // GET /merchant/products/{id}
    // =========================================================================

    @Test
    void productReturnsSingleProduct() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        Product product = product(1, "Polo", true);
        when(productService.findInStore(1, 10)).thenReturn(product);
        when(productService.toResponseDTO(product)).thenReturn(responseDTO(1));

        var result = controller.product(authentication, 1, 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((ProductResponseDTO) result.getBody()).getId()).isEqualTo(1);
    }

    // =========================================================================
    // POST /merchant/products
    // =========================================================================

    @Test
    void createProductReturns201() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        ProductRequestDTO request = new ProductRequestDTO();
        Product created = product(1, "Polo", true);
        when(productService.createForStore(store, request)).thenReturn(created);
        when(productService.toResponseDTO(created)).thenReturn(responseDTO(1));

        var result = controller.createProduct(authentication, 10, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(((ProductResponseDTO) result.getBody()).getId()).isEqualTo(1);
    }

    // =========================================================================
    // PUT /merchant/products/{id}
    // =========================================================================

    @Test
    void updateProductReturnsUpdatedProduct() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        ProductRequestDTO request = new ProductRequestDTO();
        Product existing = product(1, "Polo", true);
        Product updated = product(1, "Polo actualizado", true);
        when(productService.findInStore(1, 10)).thenReturn(existing);
        when(productService.updateForStore(existing, request)).thenReturn(updated);
        when(productService.toResponseDTO(updated)).thenReturn(responseDTO(1));

        var result = controller.updateProduct(authentication, 1, 10, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(productService).updateForStore(existing, request);
    }

    // =========================================================================
    // PATCH /merchant/products/{id}/active
    // =========================================================================

    @Test
    void updateProductActiveThrowsWhenActiveNull() {
        var result = controller.updateProductActive(
                authentication, 1, 10, new MerchantProductController.ActiveRequest(null));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateProductActiveTogglesAndReturnsUpdated() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        Product existing = product(1, "Polo", true);
        Product toggled = product(1, "Polo", false);
        when(productService.findInStore(1, 10)).thenReturn(existing);
        when(productService.toggleActive(existing, false)).thenReturn(toggled);
        when(productService.toResponseDTO(toggled)).thenReturn(responseDTO(1));

        var result = controller.updateProductActive(
                authentication, 1, 10, new MerchantProductController.ActiveRequest(false));

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(productService).toggleActive(existing, false);
    }

    // =========================================================================
    // DELETE /merchant/products/{id}
    // =========================================================================

    @Test
    void deleteProductReturnsSuccessMessage() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        Product existing = product(1, "Polo", true);
        when(productService.findInStore(1, 10)).thenReturn(existing);

        var result = controller.deleteProduct(authentication, 1, 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) result.getBody();
        assertThat(body.get("message")).isEqualTo("Product deleted successfully");
        verify(productService).delete(1);
    }

    // =========================================================================
    // POST /merchant/products/images
    // =========================================================================

    @Test
    void uploadProductImageThrowsWhenImageEmpty() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        MockMultipartFile empty = new MockMultipartFile("image", "foto.png", "image/png", new byte[0]);

        var result = controller.uploadProductImage(authentication, 10, empty);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploadProductImageThrowsWhenExtensionNotAllowed() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        MockMultipartFile file = new MockMultipartFile("image", "documento.pdf", "application/pdf", "data".getBytes());

        var result = controller.uploadProductImage(authentication, 10, file);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploadProductImageThrowsWhenTooLarge() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        byte[] tooBig = new byte[(int) (2L * 1024 * 1024 + 1)];
        MockMultipartFile file = new MockMultipartFile("image", "foto.png", "image/png", tooBig);

        var result = controller.uploadProductImage(authentication, 10, file);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void uploadProductImageSucceedsForValidImage() throws Exception {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        MockMultipartFile file = new MockMultipartFile("image", "foto.png", "image/png", "data".getBytes());
        when(productService.uploadImage(store, file, storageService))
                .thenReturn("https://bucket.s3.amazonaws.com/products/foto.png");

        var result = controller.uploadProductImage(authentication, 10, file);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) result.getBody();
        assertThat(body.get("imageUrl")).isEqualTo("https://bucket.s3.amazonaws.com/products/foto.png");
    }

    @Test
    void uploadProductImageHandlesNullFilename() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        MockMultipartFile file = new MockMultipartFile("image", null, "image/png", "data".getBytes());

        var result = controller.uploadProductImage(authentication, 10, file);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}