package pe.edu.pucp.kingstore.service.store;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

import java.util.List;
import java.util.Optional;

@Service
public class StoreService extends AbstractCrudService<Store> {

    private final StoreRepository storeRepository;

    public StoreService(StoreRepository storeRepository) {
        super(storeRepository, "Store");
        this.storeRepository = storeRepository;
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
    }

    private String normalizeSlug(String slug) {
        return slug.trim().toLowerCase().replaceAll("\\s+", "-");
    }
}
