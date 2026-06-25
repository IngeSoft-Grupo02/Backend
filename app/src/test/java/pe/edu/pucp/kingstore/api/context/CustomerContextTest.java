package pe.edu.pucp.kingstore.api.context;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.repository.user.CustomerRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.store.StoreService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerContextTest {

    @Mock private StoreService storeService;
    @Mock private CustomerRepository customerRepository;

    private CustomerContext context;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        context = new CustomerContext(storeService, customerRepository);
        authentication = mock(Authentication.class);
    }

    // ── store ─────────────────────────────────────────────────────────────────

    @Test
    void storeReturnsActiveStore() {
        Store store = new Store();
        store.setId(10);
        store.setSlug("tienda-luna");
        when(storeService.findPublicBySlug("tienda-luna")).thenReturn(Optional.of(store));

        Store result = context.store("tienda-luna");

        assertThat(result.getId()).isEqualTo(10);
    }

    @Test
    void storeThrowsWhenSlugNotFound() {
        when(storeService.findPublicBySlug("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> context.store("missing"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Store not found");
    }

    @Test
    void storeCachesResult() {
        Store store = new Store();
        store.setId(10);
        when(storeService.findPublicBySlug("tienda-luna")).thenReturn(Optional.of(store));

        context.store("tienda-luna");
        context.store("tienda-luna");

        org.mockito.Mockito.verify(storeService, org.mockito.Mockito.times(1))
                .findPublicBySlug("tienda-luna");
    }

    // ── userAccountId ─────────────────────────────────────────────────────────

    @Test
    void userAccountIdParsesTokenCorrectly() {
        when(authentication.getName()).thenReturn("5");

        assertThat(context.userAccountId(authentication)).isEqualTo(5);
    }

    @Test
    void userAccountIdThrowsWhenAuthenticationNull() {
        assertThatThrownBy(() -> context.userAccountId(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Authentication is required");
    }

    @Test
    void userAccountIdThrowsWhenNameNull() {
        when(authentication.getName()).thenReturn(null);

        assertThatThrownBy(() -> context.userAccountId(authentication))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Authentication is required");
    }

    @Test
    void userAccountIdThrowsWhenNameNotNumeric() {
        when(authentication.getName()).thenReturn("not-a-number");

        assertThatThrownBy(() -> context.userAccountId(authentication))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid authentication token");
    }

    // ── customer ──────────────────────────────────────────────────────────────

    @Test
    void customerReturnsCustomerBelongingToStore() {
        Store store = new Store();
        store.setId(10);

        Store customerStore = new Store();
        customerStore.setId(10);

        Customer customer = new Customer();
        customer.setId(1);
        customer.setStore(customerStore);

        when(authentication.getName()).thenReturn("5");
        when(customerRepository.findByUserAccountId(5)).thenReturn(Optional.of(customer));

        Customer result = context.customer(authentication, store);

        assertThat(result.getId()).isEqualTo(1);
    }

    @Test
    void customerThrowsWhenNotFound() {
        Store store = new Store();
        store.setId(10);

        when(authentication.getName()).thenReturn("5");
        when(customerRepository.findByUserAccountId(5)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> context.customer(authentication, store))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Customer profile not found");
    }

    @Test
    void customerThrowsWhenBelongsToDifferentStore() {
        Store store = new Store();
        store.setId(10);

        Store otherStore = new Store();
        otherStore.setId(99);

        Customer customer = new Customer();
        customer.setId(1);
        customer.setStore(otherStore);

        when(authentication.getName()).thenReturn("5");
        when(customerRepository.findByUserAccountId(5)).thenReturn(Optional.of(customer));

        assertThatThrownBy(() -> context.customer(authentication, store))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("does not belong to this store");
    }
}