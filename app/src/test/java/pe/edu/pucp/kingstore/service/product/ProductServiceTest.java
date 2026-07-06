package pe.edu.pucp.kingstore.service.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import pe.edu.pucp.kingstore.domain.model.product.enums.ProductStatus;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.repository.product.DiscountRepository;
import pe.edu.pucp.kingstore.repository.product.ProductRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre únicamente lo que ProductService agrega sobre AbstractCrudService:
 *   - findByStore
 *   - findActiveByStore
 *   - searchByNameInStore
 *   - validateForSave (todos sus branches)
 *
 * Los branches de AbstractCrudService (create, update, delete, requireId, etc.)
 * están cubiertos en AbstractCrudServiceTest.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @Mock
    private StockAvailabilityService stockAvailabilityService;

    private ProductService service;

    @Mock
    private DiscountRepository discountRepository;

    @BeforeEach
    void setUp() {
        service = new ProductService(productRepository, discountRepository, stockAvailabilityService);
    }

    private Product validProduct() {
        Store store = new Store();
        store.setId(10);
        Product product = new Product();
        product.setStore(store);
        product.setName("Polo básico");
        product.setCostPrice(20.0);
        product.setBasePrice(50.0);
        product.setStatus(ProductStatus.ACTIVE);
        return product;
    }

    // =========================================================================
    // findByStore
    // =========================================================================

    @Test
    void findByStore_retornaProductosDeLaTienda() {
        when(productRepository.findByStoreId(10)).thenReturn(List.of(validProduct()));

        List<Product> result = service.findByStore(10);

        assertThat(result).hasSize(1);
        verify(productRepository).findByStoreId(10);
    }

    // =========================================================================
    // findActiveByStore
    // =========================================================================

    @Test
    void findActiveByStore_consultaConActivoTrue() {
        when(productRepository.findByStoreIdAndActive(10, true)).thenReturn(List.of(validProduct()));

        List<Product> result = service.findActiveByStore(10);

        assertThat(result).hasSize(1);
        verify(productRepository).findByStoreIdAndActive(10, true);
    }

    // =========================================================================
    // searchByNameInStore
    // =========================================================================

    @Test
    void searchByNameInStore_retornaCoincidencias() {
        when(productRepository.findByNameContainingAndStoreId("polo", 10))
                .thenReturn(List.of(validProduct()));

        List<Product> result = service.searchByNameInStore("polo", 10);

        assertThat(result).hasSize(1);
    }

    @Test
    void searchByNameInStore_trimEaElNombreAntesDeConsultar() {
        when(productRepository.findByNameContainingAndStoreId("polo", 10)).thenReturn(List.of());

        service.searchByNameInStore("  polo  ", 10);

        verify(productRepository).findByNameContainingAndStoreId("polo", 10);
    }

    // =========================================================================
    // validateForSave
    // =========================================================================

    @Test
    void validateForSave_storeNull_lanzaBusinessRuleException() {
        Product product = validProduct();
        product.setStore(null);

        assertThatThrownBy(() -> service.create(product))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("store");
    }

    @Test
    void validateForSave_storeIdNull_lanzaBusinessRuleException() {
        Product product = validProduct();
        product.getStore().setId(null);

        assertThatThrownBy(() -> service.create(product))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("store");
    }

    @Test
    void validateForSave_nombreBlank_lanzaBusinessRuleException() {
        Product product = validProduct();
        product.setName("   ");

        assertThatThrownBy(() -> service.create(product))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("name");
    }

    @Test
    void validateForSave_costPriceNegativo_lanzaBusinessRuleException() {
        Product product = validProduct();
        product.setCostPrice(-1.0);

        assertThatThrownBy(() -> service.create(product))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("negative");
    }

    @Test
    void validateForSave_basePriceNegativo_lanzaBusinessRuleException() {
        Product product = validProduct();
        product.setBasePrice(-1.0);

        assertThatThrownBy(() -> service.create(product))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("negative");
    }

    @Test
    void validateForSave_basePriceMenorQueCostPrice_lanzaBusinessRuleException() {
        Product product = validProduct();
        product.setCostPrice(100.0);
        product.setBasePrice(50.0);

        assertThatThrownBy(() -> service.create(product))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cost price");
    }

    @Test
    void validateForSave_statusNull_activeTrue_seteaActive() {
        Product product = validProduct();
        product.setStatus(null);
        product.setActive(true);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product result = service.create(product);

        assertThat(result.getStatus()).isEqualTo(ProductStatus.ACTIVE);
        assertThat(result.getActive()).isTrue();
    }

    @Test
    void validateForSave_statusNull_activeFalse_seteaInactive() {
        Product product = validProduct();
        product.setStatus(null);
        product.setActive(false);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product result = service.create(product);

        assertThat(result.getStatus()).isEqualTo(ProductStatus.INACTIVE);
        assertThat(result.getActive()).isFalse();
    }

    @Test
    void validateForSave_variantConStockNegativo_lanzaBusinessRuleException() {
        Product product = validProduct();
        ProductVariant variant = new ProductVariant();
        variant.setStock(-1);
        product.setVariants(List.of(variant));

        assertThatThrownBy(() -> service.create(product))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("stock");
    }

    @Test
    void validateForSave_variantsNull_noLanzaExcepcion() {
        Product product = validProduct();
        product.setVariants(null);
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.create(product);

        verify(productRepository).save(any());
    }
}
