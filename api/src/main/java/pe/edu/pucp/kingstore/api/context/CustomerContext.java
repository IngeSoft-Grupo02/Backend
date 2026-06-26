package pe.edu.pucp.kingstore.api.context;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.repository.user.CustomerRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.store.StoreService;

@Component
@Scope(value = WebApplicationContext.SCOPE_REQUEST, proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CustomerContext {

    private final StoreService       storeService;
    private final CustomerRepository customerRepository;

    private Store    cachedStore;
    private Customer cachedCustomer;
    private Integer  cachedUserAccountId;

    public CustomerContext(StoreService storeService,
                           CustomerRepository customerRepository) {
        this.storeService       = storeService;
        this.customerRepository = customerRepository;
    }

    public Integer userAccountId(Authentication authentication) {
        if (cachedUserAccountId != null) return cachedUserAccountId;
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessRuleException("Authentication is required");
        }
        try {
            cachedUserAccountId = Integer.parseInt(authentication.getName());
            return cachedUserAccountId;
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("Invalid authentication token");
        }
    }

    public Store store(String slug) {
        if (cachedStore != null) return cachedStore;
        cachedStore = storeService.findPublicBySlug(slug)
                .orElseThrow(() -> new BusinessRuleException("Store not found: " + slug));
        return cachedStore;
    }

    public Customer customer(Authentication authentication, Store store) {
        if (cachedCustomer != null) return cachedCustomer;
        // Un UserAccount puede tener varias membresías (una por tienda). Resolver la de
        // ESTA tienda asegura el aislamiento multi-tenant: carrito/cotizaciones/pedidos
        // quedan ligados al Customer de la tienda del slug, no al de otra tienda.
        cachedCustomer = customerRepository
                .findByUserAccountIdAndStore_Id(userAccountId(authentication), store.getId())
                .orElseThrow(() -> new BusinessRuleException("Customer does not belong to this store"));
        return cachedCustomer;
    }
}