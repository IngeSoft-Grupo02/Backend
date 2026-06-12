package pe.edu.pucp.kingstore.service.product;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.pucp.kingstore.domain.dto.product.DiscountRequestDTO;
import pe.edu.pucp.kingstore.domain.model.product.Discount;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.product.enums.VolumeType;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.repository.product.DiscountRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Cubre lo que DiscountService agrega sobre AbstractCrudService:
 *  - findByProduct / findByStoreId
 *  - findInStore (vía store, vía product.store, no encontrado)
 *  - createForStore / updateForStore (applyRequest, enforceDiscountLimit, resolveActive)
 *  - deactivate (override)
 *  - toResponseDTO
 *  - validateForSave
 */
@ExtendWith(MockitoExtension.class)
class DiscountServiceTest {

    @Mock
    private DiscountRepository discountRepository;

    private DiscountService service;

    @BeforeEach
    void setUp() {
        service = new DiscountService(discountRepository);
    }

    private Store store(int id) {
        Store store = new Store();
        store.setId(id);
        return store;
    }

    private Product product(int id, Store store) {
        Product product = new Product();
        product.setId(id);
        product.setName("Polo");
        product.setStore(store);
        return product;
    }

    private DiscountRequestDTO requestDTO() {
        DiscountRequestDTO dto = new DiscountRequestDTO();
        dto.setMinQuantity(2);
        dto.setMaxQuantity(10);
        dto.setDiscountPercentage(20.0);
        return dto;
    }

    // =========================================================================
    // findByProduct / findByStoreId
    // =========================================================================

    @Test
    void findByProductReturnsRepositoryResult() {
        Discount discount = new Discount();
        when(discountRepository.findByProductId(5)).thenReturn(List.of(discount));

        assertThat(service.findByProduct(5)).containsExactly(discount);
    }

    @Test
    void findByProductThrowsWhenIdInvalid() {
        assertThatThrownBy(() -> service.findByProduct(0)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void findByStoreIdReturnsRepositoryResult() {
        Discount discount = new Discount();
        when(discountRepository.findByStoreId(10)).thenReturn(List.of(discount));

        assertThat(service.findByStoreId(10)).containsExactly(discount);
    }

    // =========================================================================
    // findInStore
    // =========================================================================

    @Test
    void findInStoreFindsByDiscountStore() {
        Discount discount = new Discount();
        discount.setId(1);
        discount.setStore(store(10));
        when(discountRepository.findById(1)).thenReturn(Optional.of(discount));

        assertThat(service.findInStore(1, 10)).isEqualTo(discount);
    }

    @Test
    void findInStoreFindsByProductStoreWhenDiscountStoreNull() {
        Discount discount = new Discount();
        discount.setId(1);
        discount.setStore(null);
        discount.setProduct(product(5, store(10)));
        when(discountRepository.findById(1)).thenReturn(Optional.of(discount));

        assertThat(service.findInStore(1, 10)).isEqualTo(discount);
    }

    @Test
    void findInStoreThrowsNotFoundWhenNeitherStoreMatches() {
        Discount discount = new Discount();
        discount.setId(1);
        discount.setStore(null);
        discount.setProduct(null);
        when(discountRepository.findById(1)).thenReturn(Optional.of(discount));

        assertThatThrownBy(() -> service.findInStore(1, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findInStoreThrowsNotFoundWhenDifferentStore() {
        Discount discount = new Discount();
        discount.setId(1);
        discount.setStore(store(99));
        when(discountRepository.findById(1)).thenReturn(Optional.of(discount));

        assertThatThrownBy(() -> service.findInStore(1, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findInStoreThrowsWhenIdsInvalid() {
        assertThatThrownBy(() -> service.findInStore(0, 10)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.findInStore(1, 0)).isInstanceOf(BusinessRuleException.class);
    }

    // =========================================================================
    // createForStore / enforceDiscountLimit
    // =========================================================================

    @Test
    void createForStoreThrowsWhenStoreAlreadyHasFiveDiscounts() {
        List<Discount> existing = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Discount d = new Discount();
            d.setId(i);
            existing.add(d);
        }
        when(discountRepository.findByStoreId(10)).thenReturn(existing);

        assertThatThrownBy(() -> service.createForStore(store(10), product(1, store(10)), requestDTO()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("up to 5 discounts");
    }

    @Test
    void createForStorePersistsDiscountWhenUnderLimit() {
        when(discountRepository.findByStoreId(10)).thenReturn(List.of());
        when(discountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product product = product(1, store(10));
        Discount saved = service.createForStore(store(10), product, requestDTO());

        assertThat(saved.getMinQuantity()).isEqualTo(2);
        assertThat(saved.getMaxQuantity()).isEqualTo(10);
        assertThat(saved.getDiscountPercentage()).isEqualTo(20.0);
        assertThat(saved.getActive()).isTrue();
        assertThat(saved.getVolumeType()).isEqualTo(VolumeType.UNIT);
        assertThat(saved.getAppliesTo()).isEqualTo("Todo el catalogo");
        assertThat(saved.getDiscountType()).isEqualTo("Porcentaje");
    }

    // =========================================================================
    // updateForStore
    // =========================================================================

    @Test
    void updateForStoreUpdatesFieldsAndPersists() {
        Discount discount = new Discount();
        discount.setId(1);
        when(discountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DiscountRequestDTO dto = requestDTO();
        dto.setName("Promo verano");
        dto.setVolumeType(VolumeType.DOZEN);
        dto.setActive(false);

        Discount updated = service.updateForStore(discount, store(10), product(1, store(10)), dto);

        assertThat(updated.getName()).isEqualTo("Promo verano");
        assertThat(updated.getVolumeType()).isEqualTo(VolumeType.DOZEN);
        assertThat(updated.getActive()).isFalse();
        assertThat(updated.getStore().getId()).isEqualTo(10);
    }

    // =========================================================================
    // deactivate (override)
    // =========================================================================

    @Test
    void deactivateSetsActiveFalseAndSaves() {
        Discount discount = new Discount();
        discount.setId(1);
        discount.setActive(true);
        when(discountRepository.findById(1)).thenReturn(Optional.of(discount));
        when(discountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Discount result = service.deactivate(1);

        assertThat(result.getActive()).isFalse();
    }

    // =========================================================================
    // applyRequest — validaciones
    // =========================================================================

    @Test
    void applyRequestThrowsWhenRequestNull() {
        assertThatThrownBy(() -> service.createForStore(store(10), product(1, store(10)), null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("request is required");
    }

    @Test
    void applyRequestThrowsWhenMinQuantityInvalid() {
        when(discountRepository.findByStoreId(10)).thenReturn(List.of());

        DiscountRequestDTO dto = requestDTO();
        dto.setMinQuantity(null);
        assertThatThrownBy(() -> service.createForStore(store(10), product(1, store(10)), dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Minimum quantity");

        dto.setMinQuantity(0);
        assertThatThrownBy(() -> service.createForStore(store(10), product(1, store(10)), dto))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void applyRequestDefaultsMaxQuantityToMinWhenNull() {
        when(discountRepository.findByStoreId(10)).thenReturn(List.of());
        when(discountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DiscountRequestDTO dto = requestDTO();
        dto.setMaxQuantity(null);
        dto.setMinQuantity(3);

        Discount saved = service.createForStore(store(10), product(1, store(10)), dto);

        assertThat(saved.getMaxQuantity()).isEqualTo(3);
    }

    @Test
    void applyRequestThrowsWhenMaxLessThanMin() {
        when(discountRepository.findByStoreId(10)).thenReturn(List.of());

        DiscountRequestDTO dto = requestDTO();
        dto.setMinQuantity(10);
        dto.setMaxQuantity(5);

        assertThatThrownBy(() -> service.createForStore(store(10), product(1, store(10)), dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining(">= minimum quantity");
    }

    @Test
    void applyRequestThrowsWhenValueNullOrNegative() {
        when(discountRepository.findByStoreId(10)).thenReturn(List.of());

        DiscountRequestDTO dto = requestDTO();
        dto.setDiscountPercentage(null);
        assertThatThrownBy(() -> service.createForStore(store(10), product(1, store(10)), dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Discount value");

        dto.setDiscountPercentage(-1.0);
        assertThatThrownBy(() -> service.createForStore(store(10), product(1, store(10)), dto))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void applyRequestThrowsWhenPercentageOver100AndTypeIsPercentage() {
        when(discountRepository.findByStoreId(10)).thenReturn(List.of());

        DiscountRequestDTO dto = requestDTO();
        dto.setDiscountPercentage(150.0);
        dto.setType("Porcentaje");

        assertThatThrownBy(() -> service.createForStore(store(10), product(1, store(10)), dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("between 0 and 100");
    }

    @Test
    void applyRequestSkipsPercentageRangeCheckWhenTypeIsNotPercentage() {
        when(discountRepository.findByStoreId(10)).thenReturn(List.of());
        when(discountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DiscountRequestDTO dto = requestDTO();
        dto.setDiscountPercentage(50.0);
        dto.setType("Monto fijo");

        Discount saved = service.createForStore(store(10), product(1, store(10)), dto);

        assertThat(saved.getDiscountType()).isEqualTo("Monto fijo");
        assertThat(saved.getDiscountPercentage()).isEqualTo(50.0);
    }

    @Test
    void applyRequestThrowsWhenAppliesToIsCategoria() {
        when(discountRepository.findByStoreId(10)).thenReturn(List.of());

        DiscountRequestDTO dto = requestDTO();
        dto.setAppliesTo("Categoría");

        assertThatThrownBy(() -> service.createForStore(store(10), product(1, store(10)), dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Category discounts");
    }

    @Test
    void applyRequestSetsAppliesToProductoWhenProductPresent() {
        when(discountRepository.findByStoreId(10)).thenReturn(List.of());
        when(discountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DiscountRequestDTO dto = requestDTO();
        dto.setAppliesTo("Producto específico");

        Discount saved = service.createForStore(store(10), product(1, store(10)), dto);

        assertThat(saved.getAppliesTo()).isEqualTo("Producto especifico");
    }

    @Test
    void applyRequestThrowsWhenAppliesToProductoButNoProduct() {
        when(discountRepository.findByStoreId(10)).thenReturn(List.of());

        DiscountRequestDTO dto = requestDTO();
        dto.setAppliesTo("Producto específico");

        assertThatThrownBy(() -> service.createForStore(store(10), null, dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Product is required");
    }

    @Test
    void applyRequestKeepsUsageCountWhenRequestUsageCountNull() {
        when(discountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Discount discount = new Discount();
        discount.setUsageCount(7);

        DiscountRequestDTO dto = requestDTO();
        dto.setUsageCount(null);

        Discount updated = service.updateForStore(discount, store(10), product(1, store(10)), dto);

        assertThat(updated.getUsageCount()).isEqualTo(7);
    }

    @Test
    void applyRequestOverridesUsageCountWhenProvided() {
        when(discountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Discount discount = new Discount();
        discount.setUsageCount(7);

        DiscountRequestDTO dto = requestDTO();
        dto.setUsageCount(3);

        Discount updated = service.updateForStore(discount, store(10), product(1, store(10)), dto);

        assertThat(updated.getUsageCount()).isEqualTo(3);
    }

    // =========================================================================
    // resolveActive
    // =========================================================================

    @Test
    void resolveActiveUsesExplicitActiveWhenPresent() {
        when(discountRepository.findByStoreId(10)).thenReturn(List.of());
        when(discountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DiscountRequestDTO dto = requestDTO();
        dto.setActive(false);

        Discount saved = service.createForStore(store(10), product(1, store(10)), dto);

        assertThat(saved.getActive()).isFalse();
    }

    @Test
    void resolveActiveDefaultsTrueWhenStatusBlank() {
        when(discountRepository.findByStoreId(10)).thenReturn(List.of());
        when(discountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DiscountRequestDTO dto = requestDTO();
        dto.setStatus("");

        Discount saved = service.createForStore(store(10), product(1, store(10)), dto);

        assertThat(saved.getActive()).isTrue();
    }

    @Test
    void resolveActiveTrueForActivaActiveOrActivo() {
        when(discountRepository.findByStoreId(10)).thenReturn(List.of());
        when(discountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        for (String status : List.of("activa", "Activo", "ACTIVE")) {
            DiscountRequestDTO dto = requestDTO();
            dto.setStatus(status);
            Discount saved = service.createForStore(store(10), product(1, store(10)), dto);
            assertThat(saved.getActive()).isTrue();
        }
    }

    @Test
    void resolveActiveFalseForOtherStatus() {
        when(discountRepository.findByStoreId(10)).thenReturn(List.of());
        when(discountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DiscountRequestDTO dto = requestDTO();
        dto.setStatus("pausada");

        Discount saved = service.createForStore(store(10), product(1, store(10)), dto);

        assertThat(saved.getActive()).isFalse();
    }

    // =========================================================================
    // toResponseDTO
    // =========================================================================

    @Test
    void toResponseDTOUsesDiscountStoreWhenPresent() {
        Discount discount = new Discount();
        discount.setId(1);
        discount.setStore(store(10));
        discount.setProduct(product(5, store(99)));
        discount.setActive(true);
        discount.setVolumeType(VolumeType.UNIT);
        discount.setMinQuantity(1);
        discount.setMaxQuantity(5);
        discount.setDiscountPercentage(10.0);
        discount.setName("Promo");
        discount.setDiscountType("Porcentaje");
        discount.setAppliesTo("Todo el catalogo");
        discount.setUsageCount(2);

        var dto = service.toResponseDTO(discount);

        assertThat(dto.getStoreId()).isEqualTo(10);
        assertThat(dto.getProductId()).isEqualTo(5);
        assertThat(dto.getProductName()).isEqualTo("Polo");
        assertThat(dto.getStatus()).isEqualTo("Activa");
    }

    @Test
    void toResponseDTOFallsBackToProductStoreWhenDiscountStoreNull() {
        Discount discount = new Discount();
        discount.setId(1);
        discount.setStore(null);
        discount.setProduct(product(5, store(20)));
        discount.setActive(false);
        discount.setVolumeType(VolumeType.UNIT);

        var dto = service.toResponseDTO(discount);

        assertThat(dto.getStoreId()).isEqualTo(20);
        assertThat(dto.getStatus()).isEqualTo("Pausada");
    }

    @Test
    void toResponseDTOHandlesNullProductAndStore() {
        Discount discount = new Discount();
        discount.setId(1);
        discount.setStore(null);
        discount.setProduct(null);
        discount.setActive(true);
        discount.setVolumeType(VolumeType.UNIT);

        var dto = service.toResponseDTO(discount);

        assertThat(dto.getStoreId()).isNull();
        assertThat(dto.getProductId()).isNull();
        assertThat(dto.getProductName()).isNull();
    }

    // =========================================================================
    // validateForSave
    // =========================================================================

    @Test
    void validateForSaveThrowsWhenProductMissing() {
        Discount discount = new Discount();
        discount.setProduct(null);
        discount.setMinQuantity(1);
        discount.setMaxQuantity(5);
        discount.setDiscountPercentage(10.0);

        assertThatThrownBy(() -> service.create(discount))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("must belong to a product");
    }

    @Test
    void validateForSaveThrowsWhenQuantitiesNotPositive() {
        Discount discount = new Discount();
        discount.setProduct(product(1, store(10)));
        discount.setMinQuantity(0);
        discount.setMaxQuantity(5);
        discount.setDiscountPercentage(10.0);

        assertThatThrownBy(() -> service.create(discount))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("must be positive");
    }

    @Test
    void validateForSaveThrowsWhenMinGreaterThanMax() {
        Discount discount = new Discount();
        discount.setProduct(product(1, store(10)));
        discount.setMinQuantity(10);
        discount.setMaxQuantity(5);
        discount.setDiscountPercentage(10.0);

        assertThatThrownBy(() -> service.create(discount))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot exceed");
    }

    @Test
    void validateForSaveThrowsWhenPercentageOutOfRange() {
        Discount discount = new Discount();
        discount.setProduct(product(1, store(10)));
        discount.setMinQuantity(1);
        discount.setMaxQuantity(5);
        discount.setDiscountPercentage(-1.0);

        assertThatThrownBy(() -> service.create(discount))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("between 0 and 100");

        discount.setDiscountPercentage(101.0);
        assertThatThrownBy(() -> service.create(discount))
                .isInstanceOf(BusinessRuleException.class);
    }
}