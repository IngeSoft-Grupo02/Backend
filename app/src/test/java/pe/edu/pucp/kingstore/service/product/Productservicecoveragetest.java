package pe.edu.pucp.kingstore.service.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import pe.edu.pucp.kingstore.domain.dto.product.ProductRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.product.ProductResponseDTO;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import pe.edu.pucp.kingstore.domain.model.product.enums.ProductStatus;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.repository.product.ProductRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.storage.StorageService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Cubre los métodos de ProductService NO cubiertos por ProductServiceTest:
 *  - findInStore
 *  - createForStore / updateForStore (applyRequest, resolveStatus, buildImageUrls, buildVariants)
 *  - toggleActive
 *  - uploadImage
 *  - toResponseDTO (resolveStatusLabel, todos los branches)
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceCoverageTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(productRepository);
    }

    private Store store() {
        Store store = new Store();
        store.setId(10);
        store.setSlug("mi-tienda");
        return store;
    }

    private Product product(int id, Store store) {
        Product product = new Product();
        product.setId(id);
        product.setStore(store);
        product.setName("Polo");
        product.setBasePrice(50.0);
        product.setCostPrice(20.0);
        product.setStatus(ProductStatus.ACTIVE);
        return product;
    }

    // =========================================================================
    // findInStore
    // =========================================================================

    @Test
    void findInStoreReturnsProductWhenBelongsToStore() {
        Product product = product(1, store());
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        assertThat(service.findInStore(1, 10)).isEqualTo(product);
    }

    @Test
    void findInStoreThrowsNotFoundWhenDifferentStore() {
        Product product = product(1, store());
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.findInStore(1, 99))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findInStoreThrowsNotFoundWhenStoreNull() {
        Product product = product(1, null);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.findInStore(1, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findInStoreThrowsWhenIdsInvalid() {
        assertThatThrownBy(() -> service.findInStore(0, 10)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.findInStore(1, 0)).isInstanceOf(BusinessRuleException.class);
    }

    // =========================================================================
    // createForStore / applyRequest / resolveStatus / buildImageUrls / buildVariants
    // =========================================================================

    private ProductRequestDTO requestDTO(String name, Double price) {
        ProductRequestDTO dto = new ProductRequestDTO();
        dto.setName(name);
        dto.setPrice(price);
        dto.setCostPrice(10.0);
        return dto;
    }

    @Test
    void applyRequestThrowsWhenRequestNull() {
        assertThatThrownBy(() -> service.createForStore(store(), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("request is required");
    }

    @Test
    void applyRequestThrowsWhenNotDraftAndNameBlank() {
        ProductRequestDTO dto = requestDTO("   ", 50.0);

        assertThatThrownBy(() -> service.createForStore(store(), dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("name");
    }

    @Test
    void applyRequestThrowsWhenNotDraftAndPriceInvalid() {
        ProductRequestDTO dto = requestDTO("Polo", null);

        assertThatThrownBy(() -> service.createForStore(store(), dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("price must be positive");

        ProductRequestDTO dto2 = requestDTO("Polo", 0.0);
        assertThatThrownBy(() -> service.createForStore(store(), dto2))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void applyRequestAllowsBlankNameAndPriceWhenDraft() {
        ProductRequestDTO dto = requestDTO("", null);
        dto.setCostPrice(0.0);
        dto.setStatus("draft");
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product saved = service.createForStore(store(), dto);

        assertThat(saved.getStatus()).isEqualTo(ProductStatus.DRAFT);
        assertThat(saved.getName()).isEqualTo("Sin nombre");
        assertThat(saved.getBasePrice()).isEqualTo(0);
        assertThat(saved.getCostPrice()).isEqualTo(0.0);
    }

    @Test
    void resolveStatusUsesExplicitStatusWhenProvided() {
        ProductRequestDTO dto = requestDTO("Polo", 50.0);
        dto.setStatus("inactivo");
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product saved = service.createForStore(store(), dto);

        assertThat(saved.getStatus()).isEqualTo(ProductStatus.INACTIVE);
        assertThat(saved.getActive()).isFalse();
    }

    @Test
    void resolveStatusReturnsInactiveWhenActiveFalseAndNoStatus() {
        ProductRequestDTO dto = requestDTO("Polo", 50.0);
        dto.setActive(false);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product saved = service.createForStore(store(), dto);

        assertThat(saved.getStatus()).isEqualTo(ProductStatus.INACTIVE);
    }

    @Test
    void resolveStatusReturnsOutOfStockWhenVariantsHaveZeroTotalStock() {
        ProductRequestDTO dto = requestDTO("Polo", 50.0);
        ProductRequestDTO.ProductVariantRequestDTO variant = new ProductRequestDTO.ProductVariantRequestDTO();
        variant.setSize("M");
        variant.setStock(0);
        dto.setVariants(List.of(variant));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product saved = service.createForStore(store(), dto);

        assertThat(saved.getStatus()).isEqualTo(ProductStatus.OUT_OF_STOCK);
    }

    @Test
    void resolveStatusReturnsActiveWhenVariantsHavePositiveStock() {
        ProductRequestDTO dto = requestDTO("Polo", 50.0);
        ProductRequestDTO.ProductVariantRequestDTO variant = new ProductRequestDTO.ProductVariantRequestDTO();
        variant.setSize("M");
        variant.setStock(5);
        variant.setColor(Color.RED);
        dto.setVariants(List.of(variant));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product saved = service.createForStore(store(), dto);

        assertThat(saved.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(saved.getVariants()).hasSize(1);
        assertThat(saved.getVariants().get(0).getColor()).isEqualTo(Color.RED);
        assertThat(saved.getVariants().get(0).getStock()).isEqualTo(5);
    }

    @Test
    void buildVariantsDefaultsColorToBlackWhenNull() {
        ProductRequestDTO dto = requestDTO("Polo", 50.0);
        ProductRequestDTO.ProductVariantRequestDTO variant = new ProductRequestDTO.ProductVariantRequestDTO();
        variant.setSize("M");
        variant.setStock(5);
        variant.setColor(null);
        dto.setVariants(List.of(variant));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product saved = service.createForStore(store(), dto);

        assertThat(saved.getVariants().get(0).getColor()).isEqualTo(Color.BLACK);
    }

    @Test
    void buildVariantsThrowsWhenSizeBlank() {
        ProductRequestDTO dto = requestDTO("Polo", 50.0);
        ProductRequestDTO.ProductVariantRequestDTO variant = new ProductRequestDTO.ProductVariantRequestDTO();
        variant.setSize("  ");
        variant.setStock(5);
        dto.setVariants(List.of(variant));

        assertThatThrownBy(() -> service.createForStore(store(), dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("size");
    }

    @Test
    void buildVariantsThrowsWhenStockNegativeOrNull() {
        ProductRequestDTO dto = requestDTO("Polo", 50.0);
        ProductRequestDTO.ProductVariantRequestDTO variant = new ProductRequestDTO.ProductVariantRequestDTO();
        variant.setSize("M");
        variant.setStock(-1);
        dto.setVariants(List.of(variant));

        assertThatThrownBy(() -> service.createForStore(store(), dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("stock");

        ProductRequestDTO.ProductVariantRequestDTO variantNullStock = new ProductRequestDTO.ProductVariantRequestDTO();
        variantNullStock.setSize("M");
        variantNullStock.setStock(null);
        dto.setVariants(List.of(variantNullStock));

        assertThatThrownBy(() -> service.createForStore(store(), dto))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void buildImageUrlsCleansFiltersAndLimitsList() {
        ProductRequestDTO dto = requestDTO("Polo", 50.0);
        dto.setImageUrls(List.of(
                "  https://cdn.test/a.png  ",
                "",
                "   ",
                "https://cdn.test/a.png", // duplicado
                "https://cdn.test/b.png",
                "https://cdn.test/c.png",
                "https://cdn.test/d.png",
                "https://cdn.test/e.png",
                "https://cdn.test/f.png" // se descarta por límite de 5
        ));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product saved = service.createForStore(store(), dto);

        assertThat(saved.getImageUrls()).hasSize(5);
        assertThat(saved.getImageUrls().get(0)).isEqualTo("https://cdn.test/a.png");
    }

    @Test
    void buildImageUrlsThrowsWhenDataUrl() {
        ProductRequestDTO dto = requestDTO("Polo", 50.0);
        dto.setImageUrls(List.of("data:image/png;base64,AAAA"));

        assertThatThrownBy(() -> service.createForStore(store(), dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("uploaded before saving");
    }

    @Test
    void buildImageUrlsThrowsWhenBlobUrl() {
        ProductRequestDTO dto = requestDTO("Polo", 50.0);
        dto.setImageUrls(List.of("blob:https://example.com/123"));

        assertThatThrownBy(() -> service.createForStore(store(), dto))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void buildImageUrlsThrowsWhenUrlTooLong() {
        String longUrl = "https://cdn.test/" + "a".repeat(250) + ".png";
        ProductRequestDTO dto = requestDTO("Polo", 50.0);
        dto.setImageUrls(List.of(longUrl));

        assertThatThrownBy(() -> service.createForStore(store(), dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("255");
    }

    @Test
    void buildImageUrlsReturnsEmptyListWhenNullOrEmpty() {
        ProductRequestDTO dto = requestDTO("Polo", 50.0);
        dto.setImageUrls(null);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product saved = service.createForStore(store(), dto);

        assertThat(saved.getImageUrls()).isEmpty();
    }

    // =========================================================================
    // updateForStore (replaceCollection con current != null)
    // =========================================================================

    @Test
    void updateForStoreReplacesExistingCollections() {
        Product existing = product(1, store());
        existing.setImageUrls(new java.util.ArrayList<>(List.of("https://cdn.test/old.png")));
        existing.setVariants(new java.util.ArrayList<>());
        existing.setAttributes(new java.util.ArrayList<>());

        ProductRequestDTO dto = requestDTO("Polo actualizado", 60.0);
        dto.setImageUrls(List.of("https://cdn.test/new.png"));

        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product updated = service.updateForStore(existing, dto);

        assertThat(updated.getName()).isEqualTo("Polo actualizado");
        assertThat(updated.getImageUrls()).containsExactly("https://cdn.test/new.png");
    }

    // =========================================================================
    // toggleActive
    // =========================================================================

    @Test
    void toggleActiveSetsActiveAndStatus() {
        Product product = product(1, store());
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product activated = service.toggleActive(product, true);
        assertThat(activated.getActive()).isTrue();
        assertThat(activated.getStatus()).isEqualTo(ProductStatus.ACTIVE);

        Product deactivated = service.toggleActive(product, false);
        assertThat(deactivated.getActive()).isFalse();
        assertThat(deactivated.getStatus()).isEqualTo(ProductStatus.INACTIVE);
    }

    // =========================================================================
    // uploadImage
    // =========================================================================

    @Test
    void uploadImageBuildsKeyAndDelegatesToStorageService() throws IOException {
        StorageService storageService = mock(StorageService.class);
        when(storageService.uploadBytes(anyString(), any(), anyString()))
                .thenReturn("https://bucket.s3.amazonaws.com/products/mi-tienda/uuid-foto.png");

        MockMultipartFile file = new MockMultipartFile(
                "image", "foto.png", "image/png", "contenido".getBytes());

        String url = service.uploadImage(store(), file, storageService);

        assertThat(url).startsWith("https://bucket.s3.amazonaws.com/products/mi-tienda/");
    }

    @Test
    void uploadImageSanitizesUnsafeFilenameAndHandlesNullName() throws IOException {
        StorageService storageService = mock(StorageService.class);
        when(storageService.uploadBytes(anyString(), any(), anyString()))
                .thenAnswer(inv -> "url:" + inv.getArgument(0));

        MockMultipartFile file = new MockMultipartFile(
                "image", null, "image/png", "contenido".getBytes());

        String url = service.uploadImage(store(), file, storageService);

        assertThat(url).startsWith("url:products/mi-tienda/");
    }

    // =========================================================================
    // toResponseDTO / resolveStatusLabel
    // =========================================================================

    @Test
    void toResponseDTOMapsVariantsAndSumsStock() {
        Product product = product(1, store());
        ProductVariant v1 = new ProductVariant();
        v1.setId(1);
        v1.setSize("S");
        v1.setColor(Color.BLUE);
        v1.setStock(3);
        ProductVariant v2 = new ProductVariant();
        v2.setId(2);
        v2.setSize("M");
        v2.setColor(Color.GREEN);
        v2.setStock(7);
        product.setVariants(List.of(v1, v2));
        product.setImageUrls(List.of("https://cdn.test/a.png"));
        product.setStatus(ProductStatus.ACTIVE);

        ProductResponseDTO dto = service.toResponseDTO(product);

        assertThat(dto.getStock()).isEqualTo(10);
        assertThat(dto.getVariants()).hasSize(2);
        assertThat(dto.getStatus()).isEqualTo("Activo");
        assertThat(dto.getStoreId()).isEqualTo(10);
        assertThat(dto.getImageUrls()).containsExactly("https://cdn.test/a.png");
    }

    @Test
    void toResponseDTOHandlesNullVariantsImageUrlsAndStore() {
        Product product = new Product();
        product.setId(1);
        product.setName("Sin tienda");
        product.setVariants(null);
        product.setImageUrls(null);
        product.setStatus(ProductStatus.DRAFT);
        product.setStore(null);

        ProductResponseDTO dto = service.toResponseDTO(product);

        assertThat(dto.getVariants()).isEmpty();
        assertThat(dto.getImageUrls()).isEmpty();
        assertThat(dto.getStock()).isEqualTo(0);
        assertThat(dto.getStoreId()).isNull();
        assertThat(dto.getStatus()).isEqualTo("Borrador");
    }

    @Test
    void resolveStatusLabelCoversAllStatuses() {
        Product product = product(1, store());

        product.setStatus(ProductStatus.OUT_OF_STOCK);
        assertThat(service.toResponseDTO(product).getStatus()).isEqualTo("Fuera de stock");

        product.setStatus(ProductStatus.INACTIVE);
        assertThat(service.toResponseDTO(product).getStatus()).isEqualTo("Inactivo");

        product.setStatus(ProductStatus.ACTIVE);
        assertThat(service.toResponseDTO(product).getStatus()).isEqualTo("Activo");

        product.setStatus(ProductStatus.DRAFT);
        assertThat(service.toResponseDTO(product).getStatus()).isEqualTo("Borrador");
    }

    @Test
    void resolveStatusLabelInfersFromActiveAndStockWhenStatusNull() {
        Product inactive = product(1, store());
        inactive.setStatus(null);
        inactive.setActive(false);
        assertThat(service.toResponseDTO(inactive).getStatus()).isEqualTo("Inactivo");

        Product outOfStock = product(2, store());
        outOfStock.setStatus(null);
        outOfStock.setActive(true);
        outOfStock.setVariants(List.of());
        assertThat(service.toResponseDTO(outOfStock).getStatus()).isEqualTo("Fuera de stock");

        Product active = product(3, store());
        active.setStatus(null);
        active.setActive(true);
        ProductVariant v = new ProductVariant();
        v.setId(1);
        v.setSize("M");
        v.setColor(Color.BLACK);
        v.setStock(2);
        active.setVariants(List.of(v));
        assertThat(service.toResponseDTO(active).getStatus()).isEqualTo("Activo");
    }
}