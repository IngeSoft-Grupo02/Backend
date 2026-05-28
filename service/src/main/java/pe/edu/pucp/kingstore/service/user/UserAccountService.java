package pe.edu.pucp.kingstore.service.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.dto.user.LoginRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.user.LoginResponseDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
import pe.edu.pucp.kingstore.domain.model.user.enums.Role;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.CustomerRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.repository.user.SystemAdministratorRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

import pe.edu.pucp.kingstore.domain.dto.user.CreateUserDTO;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.domain.model.user.SystemAdministrator;

@Service
public class UserAccountService extends AbstractCrudService<UserAccount> {

    private final UserAccountRepository userAccountRepository;
    private final CustomerRepository customerRepository;
    private final MerchantRepository merchantRepository;
    private final SystemAdministratorRepository administratorRepository;
    private final StoreRepository storeRepository;

    public UserAccountService(
            UserAccountRepository userAccountRepository,
            CustomerRepository customerRepository,
            MerchantRepository merchantRepository,
            SystemAdministratorRepository administratorRepository,
            StoreRepository storeRepository
    ) {
        super(userAccountRepository, "User account");
        this.userAccountRepository = userAccountRepository;
        this.customerRepository = customerRepository;
        this.merchantRepository = merchantRepository;
        this.administratorRepository = administratorRepository;
        this.storeRepository = storeRepository;
    }

    @Transactional(readOnly = true)
    public LoginResponseDTO authenticate(LoginRequestDTO request) {
        if (request == null) {
            throw new BusinessRuleException("Login request is required");
        }
        requireText(request.getEmail(), "Email");
        requireText(request.getPassword(), "Password");

        UserAccount account = userAccountRepository.findByEmail(normalizeEmail(request.getEmail()))
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .filter(user -> user.getPassword().equals(request.getPassword()))
                .orElseThrow(() -> new BusinessRuleException("Invalid credentials"));

        LoginResponseDTO response = new LoginResponseDTO();
        response.setId(account.getId());
        response.setEmail(account.getEmail());
        response.setRole(resolveRole(account.getId()));
        return response;
    }

    @Override
    protected void validateForSave(UserAccount account) {
        requireText(account.getEmail(), "Email");
        requireText(account.getPassword(), "Password");
        account.setEmail(normalizeEmail(account.getEmail()));

        userAccountRepository.findByEmail(account.getEmail())
                .filter(existing -> !existing.getId().equals(account.getId()))
                .ifPresent(existing -> {
                    throw new BusinessRuleException("Email is already registered");
                });
    }

    private Role resolveRole(Integer userAccountId) {
        if (customerRepository.findByUserAccountId(userAccountId).isPresent()) {
            return Role.CUSTOMER;
        }
        if (merchantRepository.findByUserAccountId(userAccountId).isPresent()) {
            return Role.MERCHANT;
        }
        if (administratorRepository.findByUserAccountId(userAccountId).isPresent()) {
            return Role.SYSTEM_ADMIN;
        }
        throw new BusinessRuleException("User account does not have a role assigned");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    /* H-Admin02 */

    @Transactional
    public UserAccount createWithRole(CreateUserDTO dto) {
        if (dto == null) throw new BusinessRuleException("Request is required");
        if (dto.getRole() == null) throw new BusinessRuleException("Role is required");

        // Crear UserAccount
        UserAccount account = new UserAccount();
        account.setEmail(dto.getEmail());
        account.setPassword(dto.getPassword());
        UserAccount created = create(account);

        // Crear perfil según rol
        switch (dto.getRole()) {
            case CUSTOMER -> {
                if(dto.getStoreId() == null)
                    throw new BusinessRuleException("Store is required for customers");
                Store store = storeRepository.findById(dto.getStoreId())  // necesitas inyectar StoreRepository
                        .orElseThrow(() -> new BusinessRuleException("Store not found"));

                Customer customer = new Customer();
                customer.setUserAccount(created);
                customer.setDocumentNumber(dto.getDocumentNumber());
                customer.setDocumentType(dto.getDocumentType());
                customer.setFirstName(dto.getFirstName());
                customer.setPaternalSurname(dto.getPaternalSurname());
                customer.setMaternalSurname(dto.getMaternalSurname());
                customer.setBirthDate(dto.getBirthDate());
                customer.setPhone(dto.getPhone());
                customer.setGender(dto.getGender());
                customer.setActive(true);
                customerRepository.save(customer);
            }
            case MERCHANT -> {
                if (dto.getRuc() == null || dto.getRuc().isBlank())
                    throw new BusinessRuleException("RUC is required for merchants");
                Merchant merchant = new Merchant();
                merchant.setUserAccount(created);
                merchant.setDocumentNumber(dto.getDocumentNumber());
                merchant.setDocumentType(dto.getDocumentType());
                merchant.setFirstName(dto.getFirstName());
                merchant.setPaternalSurname(dto.getPaternalSurname());
                merchant.setMaternalSurname(dto.getMaternalSurname());
                merchant.setBirthDate(dto.getBirthDate());
                merchant.setPhone(dto.getPhone());
                merchant.setGender(dto.getGender());
                merchant.setRuc(dto.getRuc());
                merchant.setActive(true);
                Merchant savedMerchant = merchantRepository.save(merchant);
                if(dto.getStoreId() != null){
                    Store store = storeRepository.findById(dto.getStoreId())
                            .orElseThrow(() -> new BusinessRuleException("Store not found"));
                    store.setMerchant(savedMerchant);
                    storeRepository.save(store);
                }
            }
            case SYSTEM_ADMIN -> {
                SystemAdministrator admin = new SystemAdministrator();
                admin.setUserAccount(created);
                admin.setDocumentNumber(dto.getDocumentNumber());
                admin.setDocumentType(dto.getDocumentType());
                admin.setFirstName(dto.getFirstName());
                admin.setPaternalSurname(dto.getPaternalSurname());
                admin.setMaternalSurname(dto.getMaternalSurname());
                admin.setBirthDate(dto.getBirthDate());
                admin.setPhone(dto.getPhone());
                admin.setGender(dto.getGender());
                admin.setActive(true);
                administratorRepository.save(admin);
            }
        }
        return created;
    }


    //Autenticar cliente por tenant
    @Transactional(readOnly = true)
    public LoginResponseDTO authenticateCustomer(String storeSlug, LoginRequestDTO request) {
        if (request == null) throw new BusinessRuleException("Login request is required");
        requireText(storeSlug, "Store slug");
        requireText(request.getEmail(), "Email");
        requireText(request.getPassword(), "Password");

        UserAccount account = userAccountRepository.findByEmail(normalizeEmail(request.getEmail()))
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .filter(user -> user.getPassword().equals(request.getPassword()))
                .orElseThrow(() -> new BusinessRuleException("Invalid credentials"));

        // Verificar que es cliente
        Customer customer = customerRepository.findByUserAccountId(account.getId())
                .orElseThrow(() -> new BusinessRuleException("User is not a customer"));

        LoginResponseDTO response = new LoginResponseDTO();
        response.setId(account.getId());
        response.setEmail(account.getEmail());
        response.setRole(Role.CUSTOMER);
        response.setToken(null); // el token lo genera el controller
        return response;
    }
    @Transactional
    public UserAccount updateUser(Integer id, CreateUserDTO dto) {
        UserAccount account = getById(id);

        if (dto.getEmail() != null && !dto.getEmail().isBlank()) {
            account.setEmail(normalizeEmail(dto.getEmail()));
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            account.setPassword(dto.getPassword());
        }

        validateForSave(account);
        return userAccountRepository.save(account);
    }
    @Transactional
    public UserAccount createWithRole(CreateUserDTO dto, String slug){
        Store store = storeRepository.findBySlug(slug)
                .filter(s -> s.getStoreStatus() == StoreStatus.ACTIVE)
                .orElseThrow(() -> new BusinessRuleException("Store not found or inactive"));
        dto.setStoreId(store.getId());
        return createWithRole(dto);
    }
}
