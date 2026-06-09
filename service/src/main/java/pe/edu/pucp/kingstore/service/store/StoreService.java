package pe.edu.pucp.kingstore.service.store;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.dto.store.StoreDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
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

    public StoreService(StoreRepository storeRepository,
                        MerchantRepository merchantRepository,
                        StoreCategoryRepository categoryRepository) {
        super(storeRepository, "Store");
        this.storeRepository    = storeRepository;
        this.merchantRepository = merchantRepository;
        this.categoryRepository = categoryRepository;
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

    private String normalizeSlug(String slug) {
        return slug.trim().toLowerCase().replaceAll("\\s+", "-");
    }
}
