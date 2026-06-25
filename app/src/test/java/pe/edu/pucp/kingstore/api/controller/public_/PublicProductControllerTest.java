package pe.edu.pucp.kingstore.api.controller.public_;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import pe.edu.pucp.kingstore.domain.dto.product.ProductPublicDTO;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.product.ProductService;
import pe.edu.pucp.kingstore.service.store.StoreService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicProductControllerTest {

    @Mock private StoreService   storeService;
    @Mock private ProductService productService;

    private PublicProductController controller;
    private Store store;
    private Product product;
    private ProductPublicDTO productDTO;

    @BeforeEach
    void setUp() {
        controller = new PublicProductController(storeService, productService);
        store      = new Store();
        store.setId(10);
        store.setSlug("tienda-luna");

        product = new Product();
        product.setId(1);
        product.setName("Polo Clásico");
        product.setStore(store);

        productDTO = new ProductPublicDTO(1, "Polo Clásico", "desc", 89.0,
                List.of(), List.of(), List.of());
    }

    // ── GET /stores/public/{slug}/products ────────────────────────────────────

    @Test
    void findAllReturnsPublicProductsForStore() {
        when(storeService.findPublicBySlug("tienda-luna")).thenReturn(Optional.of(store));
        when(productService.findPublicByStore(10)).thenReturn(List.of(product));
        when(productService.toPublicDTO(product)).thenReturn(productDTO);

        var result = controller.findAll("tienda-luna", null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<ProductPublicDTO> body = (List<ProductPublicDTO>) result.getBody();
        assertThat(body).hasSize(1);
        assertThat(body.get(0).getName()).isEqualTo("Polo Clásico");
    }

    @Test
    void findAllFiltersProductsBySearchTerm() {
        Product otherProduct = new Product();
        otherProduct.setId(2);
        otherProduct.setName("Casaca Negra");

        when(storeService.findPublicBySlug("tienda-luna")).thenReturn(Optional.of(store));
        when(productService.findPublicByStore(10)).thenReturn(List.of(product, otherProduct));
        when(productService.toPublicDTO(product)).thenReturn(productDTO);

        var result = controller.findAll("tienda-luna", "polo");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<ProductPublicDTO> body = (List<ProductPublicDTO>) result.getBody();
        assertThat(body).hasSize(1);
    }

    @Test
    void findAllReturns404WhenStoreNotFound() {
        when(storeService.findPublicBySlug("missing")).thenReturn(Optional.empty());

        var result = controller.findAll("missing", null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void findAllReturnsEmptyListWhenNoProducts() {
        when(storeService.findPublicBySlug("tienda-luna")).thenReturn(Optional.of(store));
        when(productService.findPublicByStore(10)).thenReturn(List.of());

        var result = controller.findAll("tienda-luna", null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<ProductPublicDTO> body = (List<ProductPublicDTO>) result.getBody();
        assertThat(body).isEmpty();
    }

    // ── GET /stores/public/{slug}/products/{id} ───────────────────────────────

    @Test
    void findByIdReturnsProductDetail() {
        when(storeService.findPublicBySlug("tienda-luna")).thenReturn(Optional.of(store));
        when(productService.findPublicInStore(1, 10)).thenReturn(product);
        when(productService.toPublicDTO(product)).thenReturn(productDTO);

        var result = controller.findById("tienda-luna", 1);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((ProductPublicDTO) result.getBody()).getName()).isEqualTo("Polo Clásico");
    }

    @Test
    void findByIdReturns404WhenProductNotFound() {
        when(storeService.findPublicBySlug("tienda-luna")).thenReturn(Optional.of(store));
        when(productService.findPublicInStore(99, 10))
                .thenThrow(new ResourceNotFoundException("Product", 99));

        var result = controller.findById("tienda-luna", 99);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void findByIdReturns404WhenStoreNotFound() {
        when(storeService.findPublicBySlug("missing")).thenReturn(Optional.empty());

        var result = controller.findById("missing", 1);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}