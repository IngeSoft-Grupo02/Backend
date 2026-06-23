package pe.edu.pucp.kingstore.service.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.pucp.kingstore.domain.model.product.CustomDesign;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.repository.product.CustomDesignRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomDesignServiceTest {

    @Mock private CustomDesignRepository customDesignRepository;

    private CustomDesignService service;

    @BeforeEach
    void setUp() {
        service = new CustomDesignService(customDesignRepository);
    }

    private Store store(Integer id) {
        Store store = new Store();
        store.setId(id);
        return store;
    }

    private Product product(Integer id, Store store) {
        Product product = new Product();
        product.setId(id);
        product.setStore(store);
        return product;
    }

    private CustomDesign design(Integer id, Product product) {
        CustomDesign design = new CustomDesign();
        design.setId(id);
        design.setProduct(product);
        design.setDescription("Diseño " + id);
        return design;
    }

    // ── findByStore ───────────────────────────────────────────────────────────

    @Test
    void findByStoreReturnsDesignsForStore() {
        Store store = store(10);
        Product product = product(1, store);
        CustomDesign design = design(1, product);
        when(customDesignRepository.findByProduct_Store_Id(10)).thenReturn(List.of(design));

        List<CustomDesign> result = service.findByStore(10);

        assertThat(result).containsExactly(design);
    }

    @Test
    void findByStoreThrowsWhenIdInvalid() {
        assertThatThrownBy(() -> service.findByStore(0))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ── findInStore ───────────────────────────────────────────────────────────

    @Test
    void findInStoreReturnsDesignBelongingToStore() {
        Store store = store(10);
        Product product = product(1, store);
        CustomDesign design = design(1, product);
        when(customDesignRepository.findById(1)).thenReturn(Optional.of(design));

        CustomDesign result = service.findInStore(1, 10);

        assertThat(result).isEqualTo(design);
    }

    @Test
    void findInStoreThrowsWhenDesignBelongsToDifferentStore() {
        Store store = store(10);
        Store otherStore = store(99);
        Product product = product(1, otherStore);
        CustomDesign design = design(1, product);
        when(customDesignRepository.findById(1)).thenReturn(Optional.of(design));

        assertThatThrownBy(() -> service.findInStore(1, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findInStoreThrowsWhenNotFound() {
        when(customDesignRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findInStore(99, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── approve ───────────────────────────────────────────────────────────────

    @Test
    void approveDesignSetsObservationsAndReviewedAt() {
        Store store = store(10);
        Product product = product(1, store);
        CustomDesign design = design(1, product);
        when(customDesignRepository.findById(1)).thenReturn(Optional.of(design));
        when(customDesignRepository.save(any(CustomDesign.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomDesign result = service.approve(1, 10);

        assertThat(result.getObservations()).isEqualTo("APPROVED");
        assertThat(result.getReviewedAt()).isNotNull();
    }

    @Test
    void approveThrowsWhenAlreadyReviewed() {
        Store store = store(10);
        Product product = product(1, store);
        CustomDesign design = design(1, product);
        design.setReviewedAt(java.time.LocalDateTime.now());
        when(customDesignRepository.findById(1)).thenReturn(Optional.of(design));

        assertThatThrownBy(() -> service.approve(1, 10))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already been reviewed");
    }

    // ── reject ────────────────────────────────────────────────────────────────

    @Test
    void rejectDesignSetsObservationsAndReviewedAt() {
        Store store = store(10);
        Product product = product(1, store);
        CustomDesign design = design(1, product);
        when(customDesignRepository.findById(1)).thenReturn(Optional.of(design));
        when(customDesignRepository.save(any(CustomDesign.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CustomDesign result = service.reject(1, 10, "No cumple requisitos");

        assertThat(result.getObservations()).isEqualTo("No cumple requisitos");
        assertThat(result.getReviewedAt()).isNotNull();
    }

    @Test
    void rejectThrowsWhenObservationsBlank() {
        assertThatThrownBy(() -> service.reject(1, 10, ""))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Observations are required");
    }

    @Test
    void rejectThrowsWhenAlreadyReviewed() {
        Store store = store(10);
        Product product = product(1, store);
        CustomDesign design = design(1, product);
        design.setReviewedAt(java.time.LocalDateTime.now());
        when(customDesignRepository.findById(1)).thenReturn(Optional.of(design));

        assertThatThrownBy(() -> service.reject(1, 10, "motivo"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already been reviewed");
    }

    // ── validateForSave ───────────────────────────────────────────────────────

    @Test
    void createThrowsWhenProductNull() {
        CustomDesign design = new CustomDesign();
        design.setProduct(null);

        assertThatThrownBy(() -> service.create(design))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("must belong to a product");
    }
}