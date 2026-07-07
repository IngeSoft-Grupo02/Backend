package pe.edu.pucp.kingstore.api.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre todos los métodos de MerchantContext, incluyendo el caching
 * por request (cachedMerchant, cachedStores, cachedUserAccountId)
 * y todos los branches de currentStore / storeById.
 */
@ExtendWith(MockitoExtension.class)
class MerchantContextTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private MerchantRepository merchantRepository;

    private MerchantContext context;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        context = new MerchantContext(storeRepository, merchantRepository);
        authentication = mock(Authentication.class);
    }

    private Store store(Integer id, boolean active, StoreStatus status) {
        Store store = new Store();
        store.setId(id);
        store.setActive(active);
        store.setStoreStatus(status);
        return store;
    }

    // =========================================================================
    // userAccountId
    // =========================================================================

    @Test
    void userAccountIdThrowsWhenAuthenticationNull() {
        assertThatThrownBy(() -> context.userAccountId(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Authenticated merchant is required");
    }

    @Test
    void userAccountIdThrowsWhenNameNull() {
        when(authentication.getName()).thenReturn(null);

        assertThatThrownBy(() -> context.userAccountId(authentication))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Authenticated merchant is required");
    }

    @Test
    void userAccountIdThrowsWhenNameNotNumeric() {
        when(authentication.getName()).thenReturn("not-a-number");

        assertThatThrownBy(() -> context.userAccountId(authentication))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("invalid");
    }

    @Test
    void userAccountIdParsesAndCachesValue() {
        when(authentication.getName()).thenReturn("7");

        assertThat(context.userAccountId(authentication)).isEqualTo(7);
        // Segunda llamada usa el cache, no vuelve a invocar authentication.getName()
        assertThat(context.userAccountId(authentication)).isEqualTo(7);
        // La primera llamada invoca getName() dos veces (null-check + parseInt)
        verify(authentication, times(2)).getName();
    }

    // =========================================================================
    // merchant
    // =========================================================================

    @Test
    void merchantThrowsWhenNotFound() {
        when(authentication.getName()).thenReturn("7");
        when(merchantRepository.findByUserAccountId(7)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> context.merchant(authentication))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("merchant profile was not found");
    }

    @Test
    void merchantReturnsAndCachesValue() {
        when(authentication.getName()).thenReturn("7");
        Merchant merchant = new Merchant();
        merchant.setId(1);
        when(merchantRepository.findByUserAccountId(7)).thenReturn(Optional.of(merchant));

        assertThat(context.merchant(authentication)).isEqualTo(merchant);
        // Segunda llamada usa el cache
        assertThat(context.merchant(authentication)).isEqualTo(merchant);
        verify(merchantRepository, times(1)).findByUserAccountId(7);
    }

    // =========================================================================
    // stores
    // =========================================================================

    @Test
    void storesFiltersInactiveAndSortsByIdWithNullsLast() {
        when(authentication.getName()).thenReturn("7");
        Store withNullId = store(null, true, StoreStatus.ACTIVE);
        Store id3 = store(3, true, StoreStatus.ACTIVE);
        Store id1 = store(1, true, StoreStatus.ACTIVE);
        Store inactive = store(2, false, StoreStatus.ACTIVE);
        Store inactiveStatus = store(4, true, StoreStatus.INACTIVE);

        when(storeRepository.findAllByMerchant_UserAccount_Id(7))
                .thenReturn(List.of(withNullId, id3, id1, inactive, inactiveStatus));

        List<Store> result = context.stores(authentication);

        assertThat(result).containsExactly(id1, id3, withNullId);
    }

    @Test
    void storesCachesResult() {
        when(authentication.getName()).thenReturn("7");
        when(storeRepository.findAllByMerchant_UserAccount_Id(7)).thenReturn(List.of());

        context.stores(authentication);
        context.stores(authentication);

        verify(storeRepository, times(1)).findAllByMerchant_UserAccount_Id(7);
    }

    // =========================================================================
    // currentStore
    // =========================================================================

    @Test
    void currentStoreThrowsWhenNoStoresAssigned() {
        when(authentication.getName()).thenReturn("7");
        when(storeRepository.findAllByMerchant_UserAccount_Id(7)).thenReturn(List.of());

        assertThatThrownBy(() -> context.currentStore(authentication, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no stores assigned");
    }

    @Test
    void currentStoreWithStoreIdReturnsMatchingStore() {
        when(authentication.getName()).thenReturn("7");
        Store store1 = store(1, true, StoreStatus.ACTIVE);
        Store store2 = store(2, true, StoreStatus.SUSPENDED);
        when(storeRepository.findAllByMerchant_UserAccount_Id(7)).thenReturn(List.of(store1, store2));

        assertThat(context.currentStore(authentication, 2)).isEqualTo(store2);
    }

    @Test
    void currentStoreWithStoreIdThrowsNotFoundWhenNoMatch() {
        when(authentication.getName()).thenReturn("7");
        Store store1 = store(1, true, StoreStatus.ACTIVE);
        when(storeRepository.findAllByMerchant_UserAccount_Id(7)).thenReturn(List.of(store1));

        assertThatThrownBy(() -> context.currentStore(authentication, 99))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void currentStoreWithoutStoreIdReturnsSingleActiveStore() {
        when(authentication.getName()).thenReturn("7");
        Store active = store(1, true, StoreStatus.ACTIVE);
        Store suspended = store(2, true, StoreStatus.SUSPENDED);
        when(storeRepository.findAllByMerchant_UserAccount_Id(7)).thenReturn(List.of(active, suspended));

        assertThat(context.currentStore(authentication, null)).isEqualTo(active);
    }

    @Test
    void currentStoreWithoutStoreIdReturnsOnlyStoreWhenNoneActive() {
        when(authentication.getName()).thenReturn("7");
        Store suspended = store(1, true, StoreStatus.SUSPENDED);
        when(storeRepository.findAllByMerchant_UserAccount_Id(7)).thenReturn(List.of(suspended));

        assertThat(context.currentStore(authentication, null)).isEqualTo(suspended);
    }

    @Test
    void currentStoreWithoutStoreIdThrowsWhenAmbiguous() {
        when(authentication.getName()).thenReturn("7");
        Store suspended1 = store(1, true, StoreStatus.SUSPENDED);
        Store suspended2 = store(2, true, StoreStatus.SUSPENDED);
        when(storeRepository.findAllByMerchant_UserAccount_Id(7)).thenReturn(List.of(suspended1, suspended2));

        assertThatThrownBy(() -> context.currentStore(authentication, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Store context is required");
    }

    @Test
    void currentStoreWithoutStoreIdThrowsWhenMultipleActiveStores() {
        when(authentication.getName()).thenReturn("7");
        Store active1 = store(1, true, StoreStatus.ACTIVE);
        Store active2 = store(2, true, StoreStatus.ACTIVE);
        when(storeRepository.findAllByMerchant_UserAccount_Id(7)).thenReturn(List.of(active1, active2));

        assertThatThrownBy(() -> context.currentStore(authentication, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Store context is required");
    }

    // =========================================================================
    // storeById
    // =========================================================================

    @Test
    void storeByIdReturnsActiveStore() {
        when(authentication.getName()).thenReturn("7");
        Store store = store(5, true, StoreStatus.ACTIVE);
        when(storeRepository.findByIdAndMerchant_UserAccount_Id(5, 7)).thenReturn(Optional.of(store));

        assertThat(context.storeById(authentication, 5)).isEqualTo(store);
    }

    @Test
    void storeByIdThrowsNotFoundWhenAbsent() {
        when(authentication.getName()).thenReturn("7");
        when(storeRepository.findByIdAndMerchant_UserAccount_Id(5, 7)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> context.storeById(authentication, 5))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void storeByIdThrowsNotFoundWhenStoreInactive() {
        when(authentication.getName()).thenReturn("7");
        Store inactive = store(5, false, StoreStatus.ACTIVE);
        when(storeRepository.findByIdAndMerchant_UserAccount_Id(5, 7)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> context.storeById(authentication, 5))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void storeByIdThrowsNotFoundWhenStoreStatusInactive() {
        when(authentication.getName()).thenReturn("7");
        Store inactive = store(5, true, StoreStatus.INACTIVE);
        when(storeRepository.findByIdAndMerchant_UserAccount_Id(5, 7)).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> context.storeById(authentication, 5))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
