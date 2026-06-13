package pe.edu.pucp.kingstore.service.store;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.dto.store.MerchantStoreRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.store.StoreDTO;
import pe.edu.pucp.kingstore.domain.dto.store.StorePublicDTO;
import pe.edu.pucp.kingstore.domain.dto.store.StoreResponseDTO;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.StoreCategory;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.repository.quotation.QuotationRepository;
import pe.edu.pucp.kingstore.repository.store.StoreCategoryRepository;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class StoreService extends AbstractCrudService<Store> {

    private final StoreRepository          storeRepository;
    private final MerchantRepository       merchantRepository;
    private final StoreCategoryRepository  categoryRepository;
    private final QuotationRepository quotationRepository;

    public StoreService(StoreRepository storeRepository,
                        MerchantRepository merchantRepository,
                        StoreCategoryRepository categoryRepository,
                        QuotationRepository quotationRepository) {
        super(storeRepository, "Store");
        this.storeRepository    = storeRepository;
        this.merchantRepository = merchantRepository;
        this.categoryRepository = categoryRepository;
        this.quotationRepository = quotationRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Store> findBySlug(String slug) {
        requireText(slug, "Store slug");
        return storeRepository.findBySlug(normalizeSlug(slug));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Store> findActive() {
        return storeRepository.findByActive(true);
    }

    @Override
    protected void validateForSave(Store store) {
        requireText(store.getStoreName(), "Store name");
        requireText(store.getSlug(), "Store slug");
        store.setSlug(normalizeSlug(store.getSlug()));

        storeRepository.findBySlug(store.getSlug())
                .filter(existing -> !existing.getId().equals(store.getId()))
                .ifPresent(existing -> {
                    throw new BusinessRuleException("Store slug is already registered");
                });

        if (store.getCategory() == null)
            throw new BusinessRuleException("Store category is required");
    }

    @Transactional(readOnly = true)
    public Optional<String> findActiveSlugByUserAccountId(Integer userAccountId) {
        return storeRepository
                .findAllByMerchant_UserAccount_IdAndStoreStatusOrderByIdAsc(userAccountId, StoreStatus.ACTIVE)
                .stream()
                .filter(store -> Boolean.TRUE.equals(store.getActive()))
                .map(Store::getSlug)
                .filter(slug -> slug != null && !slug.isBlank())
                .findFirst();
    }

    @Transactional(readOnly = true)
    public Optional<String> findLoginSlugByUserAccountId(Integer userAccountId) {
        List<Store> stores = storeRepository.findAllByMerchant_UserAccount_Id(userAccountId).stream()
                .filter(store -> Boolean.TRUE.equals(store.getActive()))
                .toList();

        return stores.stream()
                .filter(store -> store.getStoreStatus() == StoreStatus.ACTIVE)
                .findFirst()
                .or(() -> stores.stream().findFirst())
                .map(Store::getSlug)
                .filter(slug -> slug != null && !slug.isBlank());
    }

    @Transactional(readOnly = true)
    public List<Store> findByStatus(StoreStatus status) {
        return storeRepository.findByStoreStatus(status);
    }

    @Transactional(readOnly = true)
    public List<Store> findPublicStores() {
        return storeRepository.findByStoreStatus(StoreStatus.ACTIVE).stream()
                .filter(store -> Boolean.TRUE.equals(store.getActive()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Store> findPublicBySlug(String slug) {
        requireText(slug, "Store slug");
        return storeRepository.findBySlug(normalizeSlug(slug))
                .filter(store -> Boolean.TRUE.equals(store.getActive()))
                .filter(store -> store.getStoreStatus() == StoreStatus.ACTIVE);
    }

    @Transactional
    public Store createFromDTO(StoreDTO dto) {
        Store store = new Store();
        store.setStoreName(dto.getStoreName());
        store.setSlug(dto.getSlug());
        store.setDescription(dto.getDescription());
        store.setLogoUrl(dto.getLogoUrl());

        // Colores individuales
        if (dto.getPrimaryColor()   != null) store.setPrimaryColor(dto.getPrimaryColor());
        if (dto.getSecondaryColor() != null) store.setSecondaryColor(dto.getSecondaryColor());
        if (dto.getTertiaryColor()  != null) store.setTertiaryColor(dto.getTertiaryColor());

        // CategorÃ­a â€” obligatoria
        if (dto.getCategoryId() == null)
            throw new BusinessRuleException("Store category is required");
        categoryRepository.findById(dto.getCategoryId())
                .ifPresentOrElse(store::setCategory, () -> {
                    throw new BusinessRuleException("Category not found: " + dto.getCategoryId());
                });

        // Comerciante
        if (dto.getMerchantId() != null)
            merchantRepository.findById(dto.getMerchantId()).ifPresent(store::setMerchant);

        store.setStoreStatus(StoreStatus.ACTIVE);
        return create(store);
    }

    @Transactional(readOnly = true)
    public List<Store> findStores(String search, StoreStatus status) {
        List<Store> stores = status != null
                ? storeRepository.findByStoreStatus(status)
                : storeRepository.findAll();

        if (search != null && !search.isBlank()) {
            String term = search.toLowerCase();
            stores = stores.stream()
                    .filter(s -> s.getStoreName().toLowerCase().contains(term)
                            || s.getSlug().toLowerCase().contains(term))
                    .toList();
        }
        return stores;
    }

    @Transactional
    public Store suspend(Integer id) {
        Store store = getById(id);
        if (store.getStoreStatus() != StoreStatus.ACTIVE)
            throw new BusinessRuleException("Only active stores can be suspended");
        store.setStoreStatus(StoreStatus.SUSPENDED);
        return storeRepository.save(store);
    }

    @Override
    @Transactional
    public Store deactivate(Integer id) {
        Store store = getById(id);
        store.setStoreStatus(StoreStatus.INACTIVE);
        return storeRepository.save(store);
    }

    @Override
    @Transactional
    public Store reactivate(Integer id) {
        Store store = getById(id);
        store.setStoreStatus(StoreStatus.ACTIVE);
        return storeRepository.save(store);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getMetrics() {
        List<Store> all = storeRepository.findAll();
        if (all.isEmpty()) return Map.of("message", "No stores registered in the platform");

        long active    = all.stream().filter(s -> s.getStoreStatus() == StoreStatus.ACTIVE).count();
        long suspended = all.stream().filter(s -> s.getStoreStatus() == StoreStatus.SUSPENDED).count();
        long inactive  = all.stream().filter(s -> s.getStoreStatus() == StoreStatus.INACTIVE).count();

        return Map.of("total", all.size(), "active", active, "suspended", suspended, "inactive", inactive);
    }

    /**
     * Crea una tienda para un comerciante desde su panel.
     * Aplica valores por defecto de colores y categoría si no se proveen.
     */
    @Transactional
    public Store createForMerchant(Merchant merchant, MerchantStoreRequestDTO request) {
        Store store = new Store();
        store.setMerchant(merchant);
        store.setStoreStatus(StoreStatus.ACTIVE);
        applyMerchantRequest(store, request, true);
        return create(store);
    }

    /**
     * Actualiza una tienda desde el panel del comerciante.
     */
    @Transactional
    public Store updateForMerchant(Store store, MerchantStoreRequestDTO request) {
        applyMerchantRequest(store, request, false);
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            store.setStoreStatus(parseStoreStatus(request.getStatus()));
        }
        return storeRepository.save(store);
    }
    /**
     * Construye el DTO de respuesta de una tienda incluyendo el conteo
     * de cotizaciones pendientes.
     */
    @Transactional(readOnly = true)
    public StoreResponseDTO toResponseDTO(Store store) {
        long pendingQuotes = quotationRepository
                .findByStoreIdAndStatus(store.getId(), QuotationStatus.PENDING)
                .size();
        return new StoreResponseDTO(
                store.getId(),
                store.getStoreName(),
                store.getSlug(),
                store.getDescription(),
                store.getLogoUrl(),
                store.getStoreStatus() != null ? store.getStoreStatus().name() : null,
                store.getPrimaryColor() != null ? store.getPrimaryColor().name() : null,
                store.getSecondaryColor() != null ? store.getSecondaryColor().name() : null,
                store.getTertiaryColor() != null ? store.getTertiaryColor().name() : null,
                store.getCategory() != null ? store.getCategory().getId() : null,
                store.getCategory() != null ? store.getCategory().getStoreCategoryName() : null,
                pendingQuotes
        );
    }
    public StorePublicDTO toPublicDTO(Store store) {
        StorePublicDTO dto = new StorePublicDTO();
        dto.setId(store.getId());
        dto.setStoreName(store.getStoreName());
        dto.setSlug(store.getSlug());
        dto.setDescription(store.getDescription());
        dto.setLogoUrl(store.getLogoUrl());
        dto.setCategory(store.getCategory() != null
                ? store.getCategory().getStoreCategoryName() : null);
        dto.setPrimaryColor(store.getPrimaryColor());
        dto.setSecondaryColor(store.getSecondaryColor());
        dto.setTertiaryColor(store.getTertiaryColor());
        return dto;
    }
    private void applyMerchantRequest(Store store, MerchantStoreRequestDTO request, boolean creating) {
        if (request == null) throw new BusinessRuleException("Store request is required");
        requireText(request.getName(), "Store name");

        String slug = normalizeSlug(
                request.getSlug() == null || request.getSlug().isBlank()
                        ? request.getName()
                        : request.getSlug()
        );
        storeRepository.findBySlug(slug)
                .filter(existing -> !existing.getId().equals(store.getId()))
                .ifPresent(existing -> {
                    throw new BusinessRuleException("Store slug is already registered");
                });

        store.setStoreName(request.getName().trim());
        store.setSlug(slug);
        store.setDescription(blankToNull(request.getDescription()));
        store.setLogoUrl(blankToNull(request.getLogoUrl()));
        store.setPrimaryColor(request.getPrimaryColor() != null
                ? request.getPrimaryColor()
                : (store.getPrimaryColor() != null ? store.getPrimaryColor()
                   : pe.edu.pucp.kingstore.domain.model.store.enums.PrimaryColor.ONYX_BLACK));
        store.setSecondaryColor(request.getSecondaryColor() != null
                ? request.getSecondaryColor()
                : (store.getSecondaryColor() != null ? store.getSecondaryColor()
                   : pe.edu.pucp.kingstore.domain.model.store.enums.SecondaryColor.SLATE));
        store.setTertiaryColor(request.getTertiaryColor() != null
                ? request.getTertiaryColor()
                : (store.getTertiaryColor() != null ? store.getTertiaryColor()
                   : pe.edu.pucp.kingstore.domain.model.store.enums.TertiaryColor.RAW_GOLD));

        if (request.getCategoryId() != null) {
            StoreCategory category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessRuleException("Store category not found"));
            store.setCategory(category);
        } else if (creating || store.getCategory() == null) {
            StoreCategory defaultCategory = categoryRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new BusinessRuleException("Store category is required"));
            store.setCategory(defaultCategory);
        }
    }

    private StoreStatus parseStoreStatus(String value) {
        return switch (value.trim().toUpperCase().replace('-', '_').replace(' ', '_')) {
            case "ACTIVE", "ACTIVA"       -> StoreStatus.ACTIVE;
            case "INACTIVE", "INACTIVA"   -> StoreStatus.INACTIVE;
            case "SUSPENDED", "SUSPENDIDA" -> StoreStatus.SUSPENDED;
            default -> throw new BusinessRuleException("Invalid store status: " + value);
        };
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizeSlug(String slug) {
        return slug.trim().toLowerCase().replaceAll("\\s+", "-");
    }
}
