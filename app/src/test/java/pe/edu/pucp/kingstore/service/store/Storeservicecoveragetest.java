package pe.edu.pucp.kingstore.service.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.pucp.kingstore.domain.dto.store.MerchantStoreRequestDTO;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.StoreCategory;
import pe.edu.pucp.kingstore.domain.model.store.enums.PrimaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.SecondaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.domain.model.store.enums.TertiaryColor;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.repository.quotation.QuotationRepository;
import pe.edu.pucp.kingstore.repository.store.StoreCategoryRepository;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Cubre los métodos de StoreService NO cubiertos por StoreServiceTest:
 *  - createForMerchant / updateForMerchant / applyMerchantRequest (todos sus branches)
 *  - toResponseDTO
 */
@ExtendWith(MockitoExtension.class)
class StoreServiceCoverageTest {

    @Mock private StoreRepository storeRepository;
    @Mock private MerchantRepository merchantRepository;
    @Mock private StoreCategoryRepository categoryRepository;
    @Mock private QuotationRepository quotationRepository;

    private StoreService service;

    @BeforeEach
    void setUp() {
        service = new StoreService(storeRepository, merchantRepository, categoryRepository, quotationRepository);
    }

    private Store storeWithCategory(int id, String slug) {
        Store store = new Store();
        store.setId(id);
        store.setStoreName("Tienda " + id);
        store.setSlug(slug);
        store.setActive(true);
        store.setStoreStatus(StoreStatus.ACTIVE);
        StoreCategory cat = new StoreCategory();
        cat.setId(1);
        cat.setStoreCategoryName("Ropa");
        store.setCategory(cat);
        return store;
    }

    private MerchantStoreRequestDTO requestDTO() {
        MerchantStoreRequestDTO dto = new MerchantStoreRequestDTO();
        dto.setName("Mi Tienda");
        dto.setCategoryId(1);
        return dto;
    }

    private StoreCategory category(int id) {
        StoreCategory cat = new StoreCategory();
        cat.setId(id);
        cat.setStoreCategoryName("Ropa");
        return cat;
    }

    // =========================================================================
    // applyMerchantRequest — validaciones generales
    // =========================================================================

    @Test
    void createForMerchantThrowsWhenRequestNull() {
        Merchant merchant = new Merchant();
        merchant.setId(1);

        assertThatThrownBy(() -> service.createForMerchant(merchant, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("request is required");
    }

    @Test
    void createForMerchantThrowsWhenNameBlank() {
        MerchantStoreRequestDTO dto = requestDTO();
        dto.setName("   ");
        Merchant merchant = new Merchant();
        merchant.setId(1);

        assertThatThrownBy(() -> service.createForMerchant(merchant, dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Store name");
    }

    @Test
    void createForMerchantGeneratesUniqueSlugWhenBaseAlreadyRegistered() {
        MerchantStoreRequestDTO dto = requestDTO();
        dto.setSlug("slug-manual-ignorado");
        Store existing = storeWithCategory(99, "mi-tienda");
        when(storeRepository.findBySlug("mi-tienda")).thenReturn(Optional.of(existing));
        when(storeRepository.findBySlug("mi-tienda-2")).thenReturn(Optional.empty());
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category(1)));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Merchant merchant = new Merchant();
        merchant.setId(1);

        Store saved = service.createForMerchant(merchant, dto);

        assertThat(saved.getSlug()).isEqualTo("mi-tienda-2");
    }

    @Test
    void createForMerchantDerivesSlugFromNameWhenSlugBlank() {
        MerchantStoreRequestDTO dto = requestDTO();
        dto.setName("Mi Tienda Genial");
        dto.setSlug("");

        when(storeRepository.findBySlug("mi-tienda-genial")).thenReturn(Optional.empty());
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category(1)));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Merchant merchant = new Merchant();
        merchant.setId(1);

        Store saved = service.createForMerchant(merchant, dto);

        assertThat(saved.getSlug()).isEqualTo("mi-tienda-genial");
        assertThat(saved.getStoreName()).isEqualTo("Mi Tienda Genial");
        assertThat(saved.getMerchant()).isEqualTo(merchant);
        assertThat(saved.getStoreStatus()).isEqualTo(StoreStatus.ACTIVE);
    }

    // =========================================================================
    // applyMerchantRequest — colores por defecto
    // =========================================================================

    @Test
    void createForMerchantAppliesDefaultColorsWhenNotProvided() {
        MerchantStoreRequestDTO dto = requestDTO();
        dto.setSlug("mi-tienda");

        when(storeRepository.findBySlug("mi-tienda")).thenReturn(Optional.empty());
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category(1)));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Merchant merchant = new Merchant();
        merchant.setId(1);

        Store saved = service.createForMerchant(merchant, dto);

        assertThat(saved.getPrimaryColor()).isEqualTo(PrimaryColor.ONYX_BLACK);
        assertThat(saved.getSecondaryColor()).isEqualTo(SecondaryColor.SLATE);
        assertThat(saved.getTertiaryColor()).isEqualTo(TertiaryColor.RAW_GOLD);
    }

    @Test
    void createForMerchantUsesProvidedColorsWhenPresent() {
        MerchantStoreRequestDTO dto = requestDTO();
        dto.setSlug("mi-tienda");
        dto.setPrimaryColor(PrimaryColor.MIDNIGHT);
        dto.setSecondaryColor(SecondaryColor.SAGE);
        dto.setTertiaryColor(TertiaryColor.COPPER);

        when(storeRepository.findBySlug("mi-tienda")).thenReturn(Optional.empty());
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category(1)));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Merchant merchant = new Merchant();
        merchant.setId(1);

        Store saved = service.createForMerchant(merchant, dto);

        assertThat(saved.getPrimaryColor()).isEqualTo(PrimaryColor.MIDNIGHT);
        assertThat(saved.getSecondaryColor()).isEqualTo(SecondaryColor.SAGE);
        assertThat(saved.getTertiaryColor()).isEqualTo(TertiaryColor.COPPER);
    }

    @Test
    void createForMerchantPersistsDesignFeePercentageWhenProvided() {
        MerchantStoreRequestDTO dto = requestDTO();
        dto.setSlug("mi-tienda");
        dto.setDesignFeePercentage(5.0);

        when(storeRepository.findBySlug("mi-tienda")).thenReturn(Optional.empty());
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category(1)));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Merchant merchant = new Merchant();
        merchant.setId(1);

        Store saved = service.createForMerchant(merchant, dto);

        assertThat(saved.getDesignFeePercentage()).isEqualTo(5.0);
    }

    @Test
    void createForMerchantDefaultsDesignFeePercentageToTenWhenMissing() {
        MerchantStoreRequestDTO dto = requestDTO();
        dto.setSlug("mi-tienda");

        when(storeRepository.findBySlug("mi-tienda")).thenReturn(Optional.empty());
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category(1)));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Merchant merchant = new Merchant();
        merchant.setId(1);

        Store saved = service.createForMerchant(merchant, dto);

        assertThat(saved.getDesignFeePercentage()).isEqualTo(10.0);
    }

    @Test
    void createForMerchantRejectsInvalidDesignFeePercentage() {
        MerchantStoreRequestDTO dto = requestDTO();
        dto.setDesignFeePercentage(20.0);
        Merchant merchant = new Merchant();
        merchant.setId(1);

        assertThatThrownBy(() -> service.createForMerchant(merchant, dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("5, 10 or 15");
    }

    @Test
    void updateForMerchantKeepsExistingColorsWhenRequestColorsNullAndStoreHasColors() {
        Store store = storeWithCategory(5, "mi-tienda");
        store.setPrimaryColor(PrimaryColor.CHARCOAL);
        store.setSecondaryColor(SecondaryColor.TERRA);
        store.setTertiaryColor(TertiaryColor.EMERALD);

        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MerchantStoreRequestDTO dto = requestDTO();
        dto.setSlug("mi-tienda");
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category(1)));

        Store updated = service.updateForMerchant(store, dto);

        assertThat(updated.getPrimaryColor()).isEqualTo(PrimaryColor.CHARCOAL);
        assertThat(updated.getSecondaryColor()).isEqualTo(SecondaryColor.TERRA);
        assertThat(updated.getTertiaryColor()).isEqualTo(TertiaryColor.EMERALD);
    }

    // =========================================================================
    // applyMerchantRequest — descripción / logo / status
    // =========================================================================

    @Test
    void applyMerchantRequestSetsBlankDescriptionAndLogoToNull() {
        MerchantStoreRequestDTO dto = requestDTO();
        dto.setSlug("mi-tienda");
        dto.setDescription("   ");
        dto.setLogoUrl("");

        when(storeRepository.findBySlug("mi-tienda")).thenReturn(Optional.empty());
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category(1)));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Merchant merchant = new Merchant();
        merchant.setId(1);

        Store saved = service.createForMerchant(merchant, dto);

        assertThat(saved.getDescription()).isNull();
        assertThat(saved.getLogoUrl()).isNull();
    }

    @Test
    void updateForMerchantParsesStatusWhenProvided() {
        Store store = storeWithCategory(5, "mi-tienda");
        store.setStoreStatus(StoreStatus.ACTIVE);

        when(categoryRepository.findById(1)).thenReturn(Optional.of(category(1)));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MerchantStoreRequestDTO dto = requestDTO();
        dto.setSlug("mi-tienda");
        dto.setStatus("suspendida");

        Store updated = service.updateForMerchant(store, dto);

        assertThat(updated.getStoreStatus()).isEqualTo(StoreStatus.SUSPENDED);
    }

    @Test
    void updateForMerchantParsesActiveAndInactiveStatuses() {
        Store store = storeWithCategory(5, "mi-tienda");
        store.setStoreStatus(StoreStatus.SUSPENDED);

        when(categoryRepository.findById(1)).thenReturn(Optional.of(category(1)));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MerchantStoreRequestDTO dto = requestDTO();
        dto.setStatus("active");
        Store active = service.updateForMerchant(store, dto);
        assertThat(active.getStoreStatus()).isEqualTo(StoreStatus.ACTIVE);

        dto.setStatus("inactiva");
        Store inactive = service.updateForMerchant(store, dto);
        assertThat(inactive.getStoreStatus()).isEqualTo(StoreStatus.INACTIVE);
    }

    @Test
    void updateForMerchantThrowsWhenStatusInvalid() {
        Store store = storeWithCategory(5, "mi-tienda");
        MerchantStoreRequestDTO dto = requestDTO();
        dto.setStatus("pausada");
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category(1)));

        assertThatThrownBy(() -> service.updateForMerchant(store, dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid store status");
    }

    @Test
    void updateForMerchantKeepsStatusWhenBlank() {
        Store store = storeWithCategory(5, "mi-tienda");
        store.setStoreStatus(StoreStatus.ACTIVE);

        when(categoryRepository.findById(1)).thenReturn(Optional.of(category(1)));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MerchantStoreRequestDTO dto = requestDTO();
        dto.setSlug("mi-tienda");
        dto.setStatus("  ");

        Store updated = service.updateForMerchant(store, dto);

        assertThat(updated.getStoreStatus()).isEqualTo(StoreStatus.ACTIVE);
    }

    // =========================================================================
    // applyMerchantRequest — categoría
    // =========================================================================

    @Test
    void applyMerchantRequestThrowsWhenCategoryIdProvidedButNotFound() {
        MerchantStoreRequestDTO dto = requestDTO();
        dto.setSlug("mi-tienda");
        dto.setCategoryId(99);

        when(categoryRepository.findById(99)).thenReturn(Optional.empty());

        Merchant merchant = new Merchant();
        merchant.setId(1);

        assertThatThrownBy(() -> service.createForMerchant(merchant, dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Store category not found");
    }

    @Test
    void applyMerchantRequestUsesFirstAvailableCategoryWhenCreatingAndCategoryIdNull() {
        MerchantStoreRequestDTO dto = requestDTO();
        dto.setSlug("mi-tienda");
        dto.setCategoryId(null);

        when(storeRepository.findBySlug("mi-tienda")).thenReturn(Optional.empty());
        when(categoryRepository.findAll()).thenReturn(List.of(category(7)));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Merchant merchant = new Merchant();
        merchant.setId(1);

        Store saved = service.createForMerchant(merchant, dto);

        assertThat(saved.getCategory().getId()).isEqualTo(7);
    }

    @Test
    void applyMerchantRequestThrowsWhenCreatingAndNoCategoriesAvailable() {
        MerchantStoreRequestDTO dto = requestDTO();
        dto.setSlug("mi-tienda");
        dto.setCategoryId(null);

        when(categoryRepository.findAll()).thenReturn(List.of());

        Merchant merchant = new Merchant();
        merchant.setId(1);

        assertThatThrownBy(() -> service.createForMerchant(merchant, dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("category is required");
    }

    @Test
    void updateForMerchantKeepsExistingCategoryWhenCategoryIdNullAndCategoryPresent() {
        Store store = storeWithCategory(5, "mi-tienda");

        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MerchantStoreRequestDTO dto = requestDTO();
        dto.setSlug("mi-tienda");
        dto.setCategoryId(null);

        Store updated = service.updateForMerchant(store, dto);

        assertThat(updated.getCategory().getId()).isEqualTo(1);
    }

    // =========================================================================
    // toResponseDTO
    // =========================================================================

    @Test
    void toResponseDTOMapsAllFieldsAndCountsPendingQuotes() {
        Store store = storeWithCategory(5, "mi-tienda");
        store.setDescription("Descripción");
        store.setLogoUrl("http://logo.png");
        store.setPrimaryColor(PrimaryColor.MIDNIGHT);
        store.setSecondaryColor(SecondaryColor.SAGE);
        store.setTertiaryColor(TertiaryColor.COPPER);

        when(quotationRepository.countByStoreIdAndStatus(5, QuotationStatus.PENDING))
                .thenReturn(2L);

        var dto = service.toResponseDTO(store);

        assertThat(dto.getId()).isEqualTo(5);
        assertThat(dto.getName()).isEqualTo("Tienda 5");
        assertThat(dto.getSlug()).isEqualTo("mi-tienda");
        assertThat(dto.getStatus()).isEqualTo("ACTIVE");
        assertThat(dto.getPrimaryColor()).isEqualTo("MIDNIGHT");
        assertThat(dto.getSecondaryColor()).isEqualTo("SAGE");
        assertThat(dto.getTertiaryColor()).isEqualTo("COPPER");
        assertThat(dto.getCategoryId()).isEqualTo(1);
        assertThat(dto.getCategoryName()).isEqualTo("Ropa");
        assertThat(dto.getPendingQuotes()).isEqualTo(2);
        assertThat(dto.getDesignFeePercentage()).isEqualTo(10.0);
    }

    @Test
    void toResponseDTOHandlesNullColorsStatusAndCategory() {
        Store store = new Store();
        store.setId(6);
        store.setStoreName("Sin categoria");
        store.setSlug("sin-categoria");
        store.setStoreStatus(null);
        store.setCategory(null);

        when(quotationRepository.countByStoreIdAndStatus(6, QuotationStatus.PENDING))
                .thenReturn(0L);

        var dto = service.toResponseDTO(store);

        assertThat(dto.getStatus()).isNull();
        assertThat(dto.getPrimaryColor()).isNull();
        assertThat(dto.getSecondaryColor()).isNull();
        assertThat(dto.getTertiaryColor()).isNull();
        assertThat(dto.getCategoryId()).isNull();
        assertThat(dto.getCategoryName()).isNull();
        assertThat(dto.getPendingQuotes()).isEqualTo(0);
    }

    @Test
    void toPublicDTOMapsStoreFieldsAndHandlesNullCategory() {
        Store store = storeWithCategory(5, "mi-tienda");
        store.setDescription("Descripcion");
        store.setLogoUrl("https://cdn.test/logo.png");
        store.setPrimaryColor(PrimaryColor.CHARCOAL);
        store.setSecondaryColor(SecondaryColor.TERRA);
        store.setTertiaryColor(TertiaryColor.EMERALD);

        var dto = service.toPublicDTO(store);

        assertThat(dto.getId()).isEqualTo(5);
        assertThat(dto.getStoreName()).isEqualTo("Tienda 5");
        assertThat(dto.getSlug()).isEqualTo("mi-tienda");
        assertThat(dto.getDescription()).isEqualTo("Descripcion");
        assertThat(dto.getLogoUrl()).isEqualTo("https://cdn.test/logo.png");
        assertThat(dto.getCategory()).isEqualTo("Ropa");
        assertThat(dto.getPrimaryColor()).isEqualTo(PrimaryColor.CHARCOAL);
        assertThat(dto.getDesignFeePercentage()).isEqualTo(10.0);

        store.setCategory(null);
        assertThat(service.toPublicDTO(store).getCategory()).isNull();
    }

    @Test
    void generateUniqueSlugUsesSuffixAndAllowsSameStoreIdOnUpdate() {
        Store existing = storeWithCategory(9, "mi-tienda");
        when(storeRepository.findBySlug("mi-tienda")).thenReturn(Optional.of(existing));
        when(storeRepository.findBySlug("mi-tienda-2")).thenReturn(Optional.empty());

        assertThat(service.generateUniqueSlug("Mi Tienda")).isEqualTo("mi-tienda-2");

        when(storeRepository.findBySlug("mi-tienda")).thenReturn(Optional.of(existing));
        assertThat(service.generateUniqueSlugForUpdate(9, "Mi Tienda")).isEqualTo("mi-tienda");
    }

    @Test
    void updateFromDTOAppliesOptionalFieldsAndDoesNotRegenerateSlugWhenNameNull() {
        Store store = storeWithCategory(5, "mi-tienda");
        StoreCategory newCategory = category(2);
        Merchant merchant = new Merchant();
        merchant.setId(8);
        pe.edu.pucp.kingstore.domain.dto.store.StoreDTO dto = new pe.edu.pucp.kingstore.domain.dto.store.StoreDTO();
        dto.setDescription("Nueva descripcion");
        dto.setPrimaryColor(PrimaryColor.MIDNIGHT);
        dto.setSecondaryColor(SecondaryColor.SAGE);
        dto.setTertiaryColor(TertiaryColor.COPPER);
        dto.setMerchantId(8);
        dto.setCategoryId(2);

        when(storeRepository.findById(5)).thenReturn(Optional.of(store));
        when(merchantRepository.findById(8)).thenReturn(Optional.of(merchant));
        when(categoryRepository.findById(2)).thenReturn(Optional.of(newCategory));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Store updated = service.updateFromDTO(5, dto);

        assertThat(updated.getDescription()).isEqualTo("Nueva descripcion");
        assertThat(updated.getPrimaryColor()).isEqualTo(PrimaryColor.MIDNIGHT);
        assertThat(updated.getSecondaryColor()).isEqualTo(SecondaryColor.SAGE);
        assertThat(updated.getTertiaryColor()).isEqualTo(TertiaryColor.COPPER);
        assertThat(updated.getMerchant()).isSameAs(merchant);
        assertThat(updated.getCategory()).isSameAs(newCategory);
        assertThat(updated.getSlug()).isEqualTo("mi-tienda");
    }
}
