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
    void createForMerchantThrowsWhenSlugAlreadyRegistered() {
        MerchantStoreRequestDTO dto = requestDTO();
        dto.setSlug("mi-tienda");
        Store existing = storeWithCategory(99, "mi-tienda");
        when(storeRepository.findBySlug("mi-tienda")).thenReturn(Optional.of(existing));

        Merchant merchant = new Merchant();
        merchant.setId(1);

        assertThatThrownBy(() -> service.createForMerchant(merchant, dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("slug is already registered");
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
    void updateForMerchantKeepsExistingColorsWhenRequestColorsNullAndStoreHasColors() {
        Store store = storeWithCategory(5, "mi-tienda");
        store.setPrimaryColor(PrimaryColor.CHARCOAL);
        store.setSecondaryColor(SecondaryColor.TERRA);
        store.setTertiaryColor(TertiaryColor.EMERALD);

        when(storeRepository.findBySlug("mi-tienda")).thenReturn(Optional.of(store));
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

        when(storeRepository.findBySlug("mi-tienda")).thenReturn(Optional.of(store));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category(1)));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MerchantStoreRequestDTO dto = requestDTO();
        dto.setSlug("mi-tienda");
        dto.setStatus("suspendida");

        Store updated = service.updateForMerchant(store, dto);

        assertThat(updated.getStoreStatus()).isEqualTo(StoreStatus.SUSPENDED);
    }

    @Test
    void updateForMerchantKeepsStatusWhenBlank() {
        Store store = storeWithCategory(5, "mi-tienda");
        store.setStoreStatus(StoreStatus.ACTIVE);

        when(storeRepository.findBySlug("mi-tienda")).thenReturn(Optional.of(store));
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

        when(storeRepository.findBySlug("mi-tienda")).thenReturn(Optional.empty());
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

        when(storeRepository.findBySlug("mi-tienda")).thenReturn(Optional.empty());
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

        when(storeRepository.findBySlug("mi-tienda")).thenReturn(Optional.of(store));
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

        Quotation q1 = new Quotation();
        Quotation q2 = new Quotation();
        when(quotationRepository.findByStoreIdAndStatus(5, QuotationStatus.PENDING))
                .thenReturn(List.of(q1, q2));

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
    }

    @Test
    void toResponseDTOHandlesNullColorsStatusAndCategory() {
        Store store = new Store();
        store.setId(6);
        store.setStoreName("Sin categoria");
        store.setSlug("sin-categoria");
        store.setStoreStatus(null);
        store.setCategory(null);

        when(quotationRepository.findByStoreIdAndStatus(6, QuotationStatus.PENDING))
                .thenReturn(List.of());

        var dto = service.toResponseDTO(store);

        assertThat(dto.getStatus()).isNull();
        assertThat(dto.getPrimaryColor()).isNull();
        assertThat(dto.getSecondaryColor()).isNull();
        assertThat(dto.getTertiaryColor()).isNull();
        assertThat(dto.getCategoryId()).isNull();
        assertThat(dto.getCategoryName()).isNull();
        assertThat(dto.getPendingQuotes()).isEqualTo(0);
    }
}