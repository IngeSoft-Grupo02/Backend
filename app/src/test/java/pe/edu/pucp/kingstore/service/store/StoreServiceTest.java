package pe.edu.pucp.kingstore.service.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.pucp.kingstore.domain.dto.store.StoreDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.StoreCategory;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.repository.store.StoreCategoryRepository;
import pe.edu.pucp.kingstore.repository.quotation.QuotationRepository;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de StoreService.
 *
 * NO repite los branches de AbstractCrudService (create, update, delete,
 * requireId, etc.) — esos están cubiertos en ProductServiceTest.
 * Solo cubre los métodos propios de StoreService y su validateForSave.
 */
@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock private StoreRepository storeRepository;
    @Mock private MerchantRepository merchantRepository;
    @Mock private StoreCategoryRepository categoryRepository;
    @Mock private QuotationRepository quotationRepository;

    private StoreService service;

    @BeforeEach
    void setUp() {
        service = new StoreService(storeRepository, merchantRepository, categoryRepository, quotationRepository);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Store activeStore(int id, String slug) {
        Store store = new Store();
        store.setId(id);
        store.setStoreName("Tienda " + id);
        store.setSlug(slug);
        store.setActive(true);
        store.setStoreStatus(StoreStatus.ACTIVE);
        StoreCategory cat = new StoreCategory();
        cat.setId(1);
        store.setCategory(cat);
        return store;
    }

    private StoreDTO validDTO() {
        StoreDTO dto = new StoreDTO();
        dto.setStoreName("Tienda Nueva");
        dto.setSlug("tienda-nueva");
        dto.setCategoryId(1);
        return dto;
    }

    // =========================================================================
    // findBySlug
    // =========================================================================

    @Test
    void findBySlug_slugBlank_lanzaBusinessRuleException() {
        assertThatThrownBy(() -> service.findBySlug("  "))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void findBySlug_normalizaSlugAntesDeConsultar() {
        when(storeRepository.findBySlug("tienda-luna")).thenReturn(Optional.empty());

        service.findBySlug("  Tienda-Luna  ");

        verify(storeRepository).findBySlug("tienda-luna");
    }

    @Test
    void findBySlug_encontrado_retornaPresente() {
        Store store = activeStore(1, "tienda-luna");
        when(storeRepository.findBySlug("tienda-luna")).thenReturn(Optional.of(store));

        Optional<Store> result = service.findBySlug("tienda-luna");

        assertThat(result).isPresent();
    }

    // =========================================================================
    // findActive — override que usa el repositorio directamente
    // =========================================================================

    @Test
    void findActive_delegaARepositoryFindByActiveTrue() {
        when(storeRepository.findByActive(true)).thenReturn(List.of(activeStore(1, "a")));

        List<Store> result = service.findActive();

        assertThat(result).hasSize(1);
        verify(storeRepository).findByActive(true);
    }

    // =========================================================================
    // validateForSave
    // =========================================================================

    @Test
    void validateForSave_storeNameBlank_lanzaBusinessRuleException() {
        Store store = activeStore(0, "slug-ok");
        store.setStoreName("   ");

        assertThatThrownBy(() -> service.create(store))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("name");
    }

    @Test
    void validateForSave_slugBlank_lanzaBusinessRuleException() {
        Store store = activeStore(0, "  ");

        assertThatThrownBy(() -> service.create(store))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("slug");
    }

    @Test
    void validateForSave_slugDuplicadoEnOtraTienda_lanzaBusinessRuleException() {
        Store existing = activeStore(99, "slug-repetido");
        Store incoming = activeStore(0, "slug-repetido");
        incoming.setId(null);

        when(storeRepository.findBySlug("slug-repetido")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create(incoming))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("slug is already registered");
    }

    @Test
    void validateForSave_slugMismaEntidad_noLanzaExcepcion() {
        // Al hacer update de una tienda, el slug puede ser el mismo que ya tiene
        Store existing = activeStore(5, "mi-tienda");
        Store incoming = activeStore(5, "mi-tienda");

        when(storeRepository.findById(5)).thenReturn(Optional.of(existing));
        when(storeRepository.findBySlug("mi-tienda")).thenReturn(Optional.of(existing));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.update(5, incoming);

        verify(storeRepository).save(any());
    }

    @Test
    void validateForSave_categoryNull_lanzaBusinessRuleException() {
        Store store = activeStore(0, "tienda-sin-cat");
        store.setCategory(null);
        when(storeRepository.findBySlug("tienda-sin-cat")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(store))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("category is required");
    }

    // =========================================================================
    // findActiveSlugByUserAccountId
    // =========================================================================

    @Test
    void findActiveSlugByUserAccountId_conTiendaActiva_retornaSlug() {
        Store store = activeStore(1, "mi-tienda");
        when(storeRepository.findAllByMerchant_UserAccount_IdAndStoreStatusOrderByIdAsc(7, StoreStatus.ACTIVE))
                .thenReturn(List.of(store));

        Optional<String> result = service.findActiveSlugByUserAccountId(7);

        assertThat(result).contains("mi-tienda");
    }

    @Test
    void findActiveSlugByUserAccountId_sinTiendas_retornaEmpty() {
        when(storeRepository.findAllByMerchant_UserAccount_IdAndStoreStatusOrderByIdAsc(7, StoreStatus.ACTIVE))
                .thenReturn(List.of());

        Optional<String> result = service.findActiveSlugByUserAccountId(7);

        assertThat(result).isEmpty();
    }

    @Test
    void findActiveSlugByUserAccountId_tiendaActivaConSlugBlank_retornaEmpty() {
        Store store = activeStore(1, "  ");
        when(storeRepository.findAllByMerchant_UserAccount_IdAndStoreStatusOrderByIdAsc(7, StoreStatus.ACTIVE))
                .thenReturn(List.of(store));

        Optional<String> result = service.findActiveSlugByUserAccountId(7);

        assertThat(result).isEmpty();
    }

    // =========================================================================
    // findLoginSlugByUserAccountId
    // =========================================================================

    @Test
    void findLoginSlugByUserAccountId_tieneTiendaActiva_retornaEsa() {
        Store activa = activeStore(1, "activa");
        Store suspendida = activeStore(2, "suspendida");
        suspendida.setStoreStatus(StoreStatus.SUSPENDED);

        when(storeRepository.findAllByMerchant_UserAccount_Id(7))
                .thenReturn(List.of(suspendida, activa));

        Optional<String> result = service.findLoginSlugByUserAccountId(7);

        assertThat(result).contains("activa");
    }

    @Test
    void findLoginSlugByUserAccountId_sinTiendaActiva_retornaPrimeraDisponible() {
        Store suspendida = activeStore(1, "suspendida");
        suspendida.setStoreStatus(StoreStatus.SUSPENDED);

        when(storeRepository.findAllByMerchant_UserAccount_Id(7))
                .thenReturn(List.of(suspendida));

        Optional<String> result = service.findLoginSlugByUserAccountId(7);

        assertThat(result).contains("suspendida");
    }

    @Test
    void findLoginSlugByUserAccountId_sinTiendasActivas_retornaEmpty() {
        when(storeRepository.findAllByMerchant_UserAccount_Id(7)).thenReturn(List.of());

        Optional<String> result = service.findLoginSlugByUserAccountId(7);

        assertThat(result).isEmpty();
    }

    @Test
    void findLoginSlugByUserAccountId_tiendaConActivefalse_noSeIncluyeEnCandidatos() {
        // active=false significa que la tienda está deshabilitada a nivel BaseEntity
        Store inactiva = activeStore(1, "inactiva");
        inactiva.setActive(false); // filtro de active en el stream
        inactiva.setStoreStatus(StoreStatus.ACTIVE);

        when(storeRepository.findAllByMerchant_UserAccount_Id(7))
                .thenReturn(List.of(inactiva));

        Optional<String> result = service.findLoginSlugByUserAccountId(7);

        assertThat(result).isEmpty();
    }

    // =========================================================================
    // findByStatus
    // =========================================================================

    @Test
    void findByStatus_delegaARepositorio() {
        when(storeRepository.findByStoreStatus(StoreStatus.SUSPENDED))
                .thenReturn(List.of(activeStore(1, "susp")));

        List<Store> result = service.findByStatus(StoreStatus.SUSPENDED);

        assertThat(result).hasSize(1);
        verify(storeRepository).findByStoreStatus(StoreStatus.SUSPENDED);
    }

    // =========================================================================
    // createFromDTO
    // =========================================================================

    @Test
    void createFromDTO_categoryIdNull_lanzaBusinessRuleException() {
        StoreDTO dto = validDTO();
        dto.setCategoryId(null);

        assertThatThrownBy(() -> service.createFromDTO(dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("category is required");
    }

    @Test
    void createFromDTO_categoryNoEncontrada_lanzaBusinessRuleException() {
        StoreDTO dto = validDTO();
        when(categoryRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createFromDTO(dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void createFromDTO_conMerchantId_asignaMerchant() {
        StoreDTO dto = validDTO();
        dto.setMerchantId(10);

        StoreCategory cat = new StoreCategory();
        cat.setId(1);
        Merchant merchant = new Merchant();
        merchant.setId(10);

        when(categoryRepository.findById(1)).thenReturn(Optional.of(cat));
        when(merchantRepository.findById(10)).thenReturn(Optional.of(merchant));
        when(storeRepository.findBySlug("tienda-nueva")).thenReturn(Optional.empty());
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Store result = service.createFromDTO(dto);

        assertThat(result.getMerchant()).isEqualTo(merchant);
        assertThat(result.getStoreStatus()).isEqualTo(StoreStatus.ACTIVE);
    }

    @Test
    void createFromDTO_sinMerchantId_noConsultaMerchantRepository() {
        StoreDTO dto = validDTO();
        dto.setMerchantId(null);

        StoreCategory cat = new StoreCategory();
        cat.setId(1);

        when(categoryRepository.findById(1)).thenReturn(Optional.of(cat));
        when(storeRepository.findBySlug("tienda-nueva")).thenReturn(Optional.empty());
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createFromDTO(dto);

        verify(merchantRepository, never()).findById(any());
    }

    @Test
    void createFromDTO_coloresOpcionales_seAsignanSiPresentes() {
        StoreDTO dto = validDTO();
        dto.setPrimaryColor(pe.edu.pucp.kingstore.domain.model.store.enums.PrimaryColor.ONYX_BLACK);

        StoreCategory cat = new StoreCategory();
        cat.setId(1);

        when(categoryRepository.findById(1)).thenReturn(Optional.of(cat));
        when(storeRepository.findBySlug("tienda-nueva")).thenReturn(Optional.empty());
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Store result = service.createFromDTO(dto);

        assertThat(result.getPrimaryColor())
                .isEqualTo(pe.edu.pucp.kingstore.domain.model.store.enums.PrimaryColor.ONYX_BLACK);
    }

    // =========================================================================
    // findStores
    // =========================================================================

    @Test
    void findStores_conStatus_consultaPorStatus() {
        when(storeRepository.findByStoreStatus(StoreStatus.ACTIVE))
                .thenReturn(List.of(activeStore(1, "a"), activeStore(2, "b")));

        List<Store> result = service.findStores(null, StoreStatus.ACTIVE);

        assertThat(result).hasSize(2);
        verify(storeRepository).findByStoreStatus(StoreStatus.ACTIVE);
    }

    @Test
    void findStores_sinStatus_consultaFindAll() {
        when(storeRepository.findAll()).thenReturn(List.of(activeStore(1, "a")));

        service.findStores(null, null);

        verify(storeRepository).findAll();
    }

    @Test
    void findStores_conSearch_filtraPorNombreOSlug() {
        Store coincide = activeStore(1, "polo-urbano");
        coincide.setStoreName("Polo Urbano");
        Store noCoincide = activeStore(2, "formal-wear");
        noCoincide.setStoreName("Formal Wear");

        when(storeRepository.findAll()).thenReturn(List.of(coincide, noCoincide));

        List<Store> result = service.findStores("polo", null);

        assertThat(result).containsExactly(coincide);
    }

    @Test
    void findStores_searchBlank_noFiltra() {
        when(storeRepository.findAll()).thenReturn(List.of(activeStore(1, "a"), activeStore(2, "b")));

        List<Store> result = service.findStores("   ", null);

        assertThat(result).hasSize(2);
    }

    // =========================================================================
    // suspend
    // =========================================================================

    @Test
    void suspend_tiendaActiva_cambiaStatusASuspended() {
        Store store = activeStore(5, "mi-tienda");
        when(storeRepository.findById(5)).thenReturn(Optional.of(store));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Store result = service.suspend(5);

        assertThat(result.getStoreStatus()).isEqualTo(StoreStatus.SUSPENDED);
    }

    @Test
    void suspend_tiendaSuspendida_lanzaBusinessRuleException() {
        Store store = activeStore(5, "mi-tienda");
        store.setStoreStatus(StoreStatus.SUSPENDED);
        when(storeRepository.findById(5)).thenReturn(Optional.of(store));

        assertThatThrownBy(() -> service.suspend(5))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("active stores can be suspended");
    }

    @Test
    void suspend_tiendaInactiva_lanzaBusinessRuleException() {
        Store store = activeStore(5, "mi-tienda");
        store.setStoreStatus(StoreStatus.INACTIVE);
        when(storeRepository.findById(5)).thenReturn(Optional.of(store));

        assertThatThrownBy(() -> service.suspend(5))
                .isInstanceOf(BusinessRuleException.class);
    }

    // =========================================================================
    // deactivate / reactivate — overrides de StoreService
    // =========================================================================

    @Test
    void deactivate_cambiaStatusAInactive() {
        Store store = activeStore(5, "mi-tienda");
        when(storeRepository.findById(5)).thenReturn(Optional.of(store));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Store result = service.deactivate(5);

        assertThat(result.getStoreStatus()).isEqualTo(StoreStatus.INACTIVE);
    }

    @Test
    void reactivate_cambiaStatusAActive() {
        Store store = activeStore(5, "mi-tienda");
        store.setStoreStatus(StoreStatus.INACTIVE);
        when(storeRepository.findById(5)).thenReturn(Optional.of(store));
        when(storeRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Store result = service.reactivate(5);

        assertThat(result.getStoreStatus()).isEqualTo(StoreStatus.ACTIVE);
    }

    // =========================================================================
    // getMetrics
    // =========================================================================

    @Test
    void getMetrics_sinTiendas_retornaMensaje() {
        when(storeRepository.findAll()).thenReturn(List.of());

        Map<String, Object> result = service.getMetrics();

        assertThat(result).containsKey("message");
    }

    @Test
    void getMetrics_conTiendas_retornaTotalesPorStatus() {
        Store activa = activeStore(1, "a");
        Store suspendida = activeStore(2, "b");
        suspendida.setStoreStatus(StoreStatus.SUSPENDED);
        Store inactiva = activeStore(3, "c");
        inactiva.setStoreStatus(StoreStatus.INACTIVE);

        when(storeRepository.findAll()).thenReturn(List.of(activa, suspendida, inactiva));

        Map<String, Object> result = service.getMetrics();

        assertThat(result.get("total")).isEqualTo(3);
        assertThat(result.get("active")).isEqualTo(1L);
        assertThat(result.get("suspended")).isEqualTo(1L);
        assertThat(result.get("inactive")).isEqualTo(1L);
    }

    @Test
    void getMetrics_todasActivas_suspendedEInactiveEnCero() {
        when(storeRepository.findAll()).thenReturn(
                List.of(activeStore(1, "a"), activeStore(2, "b")));

        Map<String, Object> result = service.getMetrics();

        assertThat(result.get("active")).isEqualTo(2L);
        assertThat(result.get("suspended")).isEqualTo(0L);
        assertThat(result.get("inactive")).isEqualTo(0L);
    }
}