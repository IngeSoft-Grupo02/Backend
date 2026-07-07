package pe.edu.pucp.kingstore.api.context;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class MerchantContext {

    private final StoreRepository storeRepository;
    private final MerchantRepository merchantRepository;

    // cache por request — se inicializan una sola vez
    private Merchant cachedMerchant;
    private List<Store> cachedStores;
    private Integer cachedUserAccountId;

    public MerchantContext(StoreRepository storeRepository,
                           MerchantRepository merchantRepository) {
        this.storeRepository = storeRepository;
        this.merchantRepository = merchantRepository;
    }

    public Integer userAccountId(Authentication authentication) {
        if (cachedUserAccountId != null) return cachedUserAccountId;
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessRuleException("Authenticated merchant is required");
        }
        try {
            cachedUserAccountId = Integer.parseInt(authentication.getName());
            return cachedUserAccountId;
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("Authenticated merchant id is invalid");
        }
    }

    public Merchant merchant(Authentication authentication) {
        if (cachedMerchant != null) return cachedMerchant;
        cachedMerchant = merchantRepository.findByUserAccountId(userAccountId(authentication))
                .orElseThrow(() -> new BusinessRuleException("Authenticated merchant profile was not found"));
        return cachedMerchant;
    }

    public List<Store> stores(Authentication authentication) {
        if (cachedStores != null) return cachedStores;
        cachedStores = storeRepository
                .findAllByMerchant_UserAccount_Id(userAccountId(authentication))
                .stream()
                .filter(store -> Boolean.TRUE.equals(store.getActive()))
                .filter(store -> store.getStoreStatus() != StoreStatus.INACTIVE)
                .sorted(Comparator.comparing(Store::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        return cachedStores;
    }

    public Store currentStore(Authentication authentication, Integer storeId) {
        List<Store> stores = stores(authentication);
        if (stores.isEmpty()) {
            throw new BusinessRuleException("Merchant has no stores assigned");
        }
        if (storeId != null) {
            return stores.stream()
                    .filter(store -> Objects.equals(store.getId(), storeId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));
        }
        List<Store> activeStores = stores.stream()
                .filter(store -> store.getStoreStatus() == StoreStatus.ACTIVE)
                .toList();
        if (activeStores.size() == 1) return activeStores.get(0);
        if (stores.size() == 1) return stores.get(0);
        throw new BusinessRuleException("Store context is required");
    }

    public Store storeById(Authentication authentication, Integer storeId) {
        Integer userAccountId = userAccountId(authentication);
        return storeRepository.findByIdAndMerchant_UserAccount_Id(storeId, userAccountId)
                .filter(store -> Boolean.TRUE.equals(store.getActive()))
                .filter(store -> store.getStoreStatus() != StoreStatus.INACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Store", storeId));
    }
}
