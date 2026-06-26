package pe.edu.pucp.kingstore.service.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.pucp.kingstore.domain.dto.user.CreateUserDTO;
import pe.edu.pucp.kingstore.domain.dto.user.LoginRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.user.RegisterCustomerDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.domain.model.user.SystemAdministrator;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
import pe.edu.pucp.kingstore.domain.model.user.enums.DocumentType;
import pe.edu.pucp.kingstore.domain.model.user.enums.Gender;
import pe.edu.pucp.kingstore.domain.model.user.enums.Role;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.CustomerRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.repository.user.SystemAdministratorRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountPasswordHashingTest {

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private MerchantRepository merchantRepository;
    @Mock private SystemAdministratorRepository administratorRepository;
    @Mock private StoreRepository storeRepository;

    private UserAccountService service;

    @BeforeEach
    void setUp() {
        service = new UserAccountService(
                userAccountRepository,
                customerRepository,
                merchantRepository,
                administratorRepository,
                storeRepository
        );
    }

    @Test
    void authenticateAcceptsHashedStoredPassword() {
        PasswordHashService hashService = new PasswordHashService();
        UserAccount account = account(1, "admin@test.com", hashService.hash("Admin123*"));
        when(userAccountRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(account));
        when(customerRepository.existsByUserAccountId(1)).thenReturn(true);

        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("admin@test.com");
        request.setPassword("Admin123*");

        assertThat(service.authenticate(request).getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void createWithRoleHashesAdminPassword() {
        when(userAccountRepository.findByEmail("admin@test.com")).thenReturn(Optional.empty());
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount saved = invocation.getArgument(0);
            saved.setId(10);
            return saved;
        });
        when(administratorRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UserAccount created = service.createWithRole(createUser(Role.SYSTEM_ADMIN, "Admin123*"));

        assertThat(created.getPassword())
                .startsWith("$2")
                .isNotEqualTo("Admin123*");
        verify(administratorRepository).save(any());
    }

    @Test
    void updateUserHashesNewPasswordOnlyOnce() {
        UserAccount existing = account(20, "user@test.com", "Old12345*");
        when(userAccountRepository.findById(20)).thenReturn(Optional.of(existing));
        when(userAccountRepository.findByEmail("user@test.com")).thenReturn(Optional.of(existing));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateUserDTO update = new CreateUserDTO();
        update.setPassword("Nueva123*");

        UserAccount updated = service.updateUser(20, update);

        assertThat(updated.getPassword())
                .startsWith("$2")
                .isNotEqualTo("Nueva123*");
        String firstHash = updated.getPassword();
        service.updateUser(20, update);
        assertThat(existing.getPassword()).isNotEqualTo(firstHash);
    }

    @Test
    void registerCustomerCreatesHashedAccountAndReusesHashedExistingAccount() {
        Store store = store();
        when(storeRepository.findBySlug("tienda")).thenReturn(Optional.of(store));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount saved = invocation.getArgument(0);
            saved.setId(30);
            return saved;
        });
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserAccount created = service.registerCustomer(customerDto("cliente@test.com", "Password123!"), "tienda");

        assertThat(created.getPassword())
                .startsWith("$2")
                .isNotEqualTo("Password123!");

        RegisterCustomerDTO secondStoreDto = customerDto("cliente@test.com", "Password123!");
        secondStoreDto.setDocumentNumber("87654321");
        when(userAccountRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(created));
        when(customerRepository.existsByUserAccountId(30)).thenReturn(true);

        UserAccount reused = service.registerCustomer(secondStoreDto, "tienda");

        assertThat(reused).isSameAs(created);
    }

    @Test
    void authenticateCustomerAcceptsHashedStoredPassword() {
        PasswordHashService hashService = new PasswordHashService();
        UserAccount account = account(40, "cliente@test.com", hashService.hash("Password123!"));
        Store store = store();
        Customer customer = new Customer();
        customer.setUserAccount(account);
        customer.setStore(store);
        when(userAccountRepository.findByEmail("cliente@test.com")).thenReturn(Optional.of(account));
        when(customerRepository.findByUserAccountIdAndStore_Slug(40, "tienda")).thenReturn(Optional.of(customer));

        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail("cliente@test.com");
        request.setPassword("Password123!");

        assertThat(service.authenticateCustomer("tienda", request).getStoreSlug()).isEqualTo("tienda");
    }

    @Test
    void authenticateRejectsInactiveAccountsBeforePasswordCheck() {
        UserAccount account = account(41, "inactive@test.com", "irrelevant");
        account.setActive(false);
        when(userAccountRepository.findByEmail("inactive@test.com")).thenReturn(Optional.of(account));

        LoginRequestDTO request = login("inactive@test.com", "Password123!");

        assertThatThrownBy(() -> service.authenticate(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("ACCOUNT_INACTIVE");
    }

    @Test
    void authenticateRejectsUnknownAccount() {
        when(userAccountRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate(login("missing@test.com", "Password123!")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("ACCOUNT_NOT_FOUND");
    }

    @Test
    void authenticateRejectsBadPasswordAndMissingRole() {
        PasswordHashService hashService = new PasswordHashService();
        UserAccount account = account(42, "user@test.com", hashService.hash("Password123!"));
        when(userAccountRepository.findByEmail("user@test.com")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.authenticate(login("user@test.com", "Wrong123!")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("BAD_CREDENTIALS");

        when(customerRepository.existsByUserAccountId(42)).thenReturn(false);
        when(merchantRepository.findByUserAccountId(42)).thenReturn(Optional.empty());
        when(administratorRepository.findByUserAccountId(42)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.authenticate(login("user@test.com", "Password123!")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("ROLE_NOT_ASSIGNED");
    }

    @Test
    void authenticateResolvesMerchantAndAdminRoles() {
        PasswordHashService hashService = new PasswordHashService();
        UserAccount merchantAccount = account(43, "merchant@test.com", hashService.hash("Password123!"));
        Merchant inactiveMerchant = new Merchant();
        inactiveMerchant.setUserAccount(merchantAccount);
        inactiveMerchant.setActive(false);
        when(userAccountRepository.findByEmail("merchant@test.com")).thenReturn(Optional.of(merchantAccount));
        when(customerRepository.existsByUserAccountId(43)).thenReturn(false);
        when(merchantRepository.findByUserAccountId(43)).thenReturn(Optional.of(inactiveMerchant));

        assertThatThrownBy(() -> service.authenticate(login("merchant@test.com", "Password123!")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("ACCOUNT_INACTIVE");

        UserAccount adminAccount = account(44, "admin2@test.com", hashService.hash("Password123!"));
        SystemAdministrator admin = new SystemAdministrator();
        admin.setUserAccount(adminAccount);
        when(userAccountRepository.findByEmail("admin2@test.com")).thenReturn(Optional.of(adminAccount));
        when(customerRepository.existsByUserAccountId(44)).thenReturn(false);
        when(merchantRepository.findByUserAccountId(44)).thenReturn(Optional.empty());
        when(administratorRepository.findByUserAccountId(44)).thenReturn(Optional.of(admin));

        assertThat(service.authenticate(login("admin2@test.com", "Password123!")).getRole())
                .isEqualTo(Role.SYSTEM_ADMIN);
    }

    @Test
    void authenticateCustomerRejectsNullRequestInvalidCredentialsAndWrongStore() {
        assertThatThrownBy(() -> service.authenticateCustomer("tienda", null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Login request is required");

        UserAccount account = account(45, "cliente2@test.com", new PasswordHashService().hash("Password123!"));
        when(userAccountRepository.findByEmail("cliente2@test.com")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> service.authenticateCustomer("tienda", login("cliente2@test.com", "Wrong123!")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Invalid credentials");

        assertThatThrownBy(() -> service.authenticateCustomer("tienda", login("cliente2@test.com", "Password123!")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Customer does not belong to this store");
    }

    @Test
    void getCustomerProfileMapsNullAccountAndRejectsWrongStore() {
        Customer customer = new Customer();
        customer.setId(11);
        customer.setStore(store());
        customer.setFirstName("Ana");
        customer.setPaternalSurname("Lopez");
        customer.setMaternalSurname("Diaz");
        customer.setPhone("999999999");
        customer.setDocumentType(DocumentType.DNI);
        customer.setDocumentNumber("12345678");
        when(customerRepository.findByUserAccountIdAndStore_Slug(77, "tienda")).thenReturn(Optional.of(customer));

        assertThat(service.getCustomerProfile(77, " tienda ").getEmail()).isNull();

        when(customerRepository.findByUserAccountIdAndStore_Slug(77, "otra")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getCustomerProfile(77, "otra"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Customer does not belong to this store");
    }

    @Test
    void createWithRoleCreatesCustomerAndMerchantProfiles() {
        Store store = store();
        when(userAccountRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount saved = invocation.getArgument(0);
            saved.setId(saved.getId() == null ? 50 : saved.getId());
            return saved;
        });
        when(storeRepository.findById(5)).thenReturn(Optional.of(store));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(invocation -> {
            Merchant saved = invocation.getArgument(0);
            saved.setId(9);
            return saved;
        });

        CreateUserDTO customer = createUser(Role.CUSTOMER, "Customer123!");
        customer.setStoreId(5);
        assertThat(service.createWithRole(customer).getId()).isEqualTo(50);

        CreateUserDTO merchant = createUser(Role.MERCHANT, "Merchant123!");
        merchant.setRuc("20123456789");
        merchant.setStoreId(5);
        assertThat(service.createWithRole(merchant).getId()).isEqualTo(50);

        assertThat(store.getMerchant()).isNotNull();
    }

    @Test
    void createWithRoleRejectsMissingRoleSpecificFields() {
        assertThatThrownBy(() -> service.createWithRole(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Request is required");

        CreateUserDTO noRole = createUser(null, "Password123!");
        assertThatThrownBy(() -> service.createWithRole(noRole))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Role is required");

        when(userAccountRepository.findByEmail(any())).thenReturn(Optional.empty());
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThatThrownBy(() -> service.createWithRole(createUser(Role.CUSTOMER, "Password123!")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Store is required for customers");

        assertThatThrownBy(() -> service.createWithRole(createUser(Role.MERCHANT, "Password123!")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("RUC is required for merchants");
    }

    @Test
    void registerCustomerRejectsInvalidPersonalDataBeforeRepositoryAccess() {
        assertThatThrownBy(() -> service.registerCustomer(null, "tienda"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Registration data is required");

        RegisterCustomerDTO invalidEmail = customerDto("bad-mail", "Password123!");
        assertThatThrownBy(() -> service.registerCustomer(invalidEmail, "tienda"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Ingresa un correo electrónico válido");

        RegisterCustomerDTO weakPassword = customerDto("cliente@test.com", "short");
        assertThatThrownBy(() -> service.registerCustomer(weakPassword, "tienda"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La contraseña debe tener al menos 8 caracteres");

        RegisterCustomerDTO badName = customerDto("cliente@test.com", "Password123!");
        badName.setFirstName("A1");
        assertThatThrownBy(() -> service.registerCustomer(badName, "tienda"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El nombre solo puede contener letras, espacios, guiones y apóstrofes");

        RegisterCustomerDTO futureBirthDate = customerDto("cliente@test.com", "Password123!");
        futureBirthDate.setBirthDate(LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> service.registerCustomer(futureBirthDate, "tienda"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La fecha de nacimiento no puede ser una fecha futura");

        RegisterCustomerDTO missingDocumentType = customerDto("cliente@test.com", "Password123!");
        missingDocumentType.setDocumentType(null);
        assertThatThrownBy(() -> service.registerCustomer(missingDocumentType, "tienda"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El tipo de documento es obligatorio");

        RegisterCustomerDTO missingBirthDate = customerDto("cliente@test.com", "Password123!");
        missingBirthDate.setBirthDate(null);
        assertThatThrownBy(() -> service.registerCustomer(missingBirthDate, "tienda"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La fecha de nacimiento es obligatoria");

        RegisterCustomerDTO missingGender = customerDto("cliente@test.com", "Password123!");
        missingGender.setGender(null);
        assertThatThrownBy(() -> service.registerCustomer(missingGender, "tienda"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El género es obligatorio");

        RegisterCustomerDTO badForeignId = customerDto("cliente@test.com", "Password123!");
        badForeignId.setDocumentType(DocumentType.FOREIGN_ID_CARD);
        badForeignId.setDocumentNumber("123");
        assertThatThrownBy(() -> service.registerCustomer(badForeignId, "tienda"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El carné de extranjería debe tener entre 9 y 15 dígitos");

        RegisterCustomerDTO badPassport = customerDto("cliente@test.com", "Password123!");
        badPassport.setDocumentType(DocumentType.PASSPORT);
        badPassport.setDocumentNumber("123");
        assertThatThrownBy(() -> service.registerCustomer(badPassport, "tienda"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El pasaporte debe tener entre 6 y 20 dígitos");
    }

    @Test
    void registerCustomerRejectsDuplicateStoreMembershipAndWrongExistingPassword() {
        Store store = store();
        when(storeRepository.findBySlug("tienda")).thenReturn(Optional.of(store));
        when(customerRepository.existsByStore_IdAndUserAccount_Email(5, "cliente@test.com")).thenReturn(true);

        assertThatThrownBy(() -> service.registerCustomer(customerDto("cliente@test.com", "Password123!"), "tienda"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El correo ya está registrado en esta tienda.");

        when(customerRepository.existsByStore_IdAndUserAccount_Email(5, "otro@test.com")).thenReturn(false);
        when(customerRepository.existsByStore_IdAndDocumentNumber(5, "12345678")).thenReturn(true);
        assertThatThrownBy(() -> service.registerCustomer(customerDto("otro@test.com", "Password123!"), "tienda"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El DNI ya está registrado en esta tienda.");

        UserAccount existing = account(60, "nuevo@test.com", new PasswordHashService().hash("Password123!"));
        RegisterCustomerDTO dto = customerDto("nuevo@test.com", "Other123!");
        dto.setDocumentNumber("87654321");
        when(customerRepository.existsByStore_IdAndDocumentNumber(5, "87654321")).thenReturn(false);
        when(userAccountRepository.findByEmail("nuevo@test.com")).thenReturn(Optional.of(existing));
        when(customerRepository.existsByUserAccountId(60)).thenReturn(true);
        assertThatThrownBy(() -> service.registerCustomer(dto, "tienda"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La contraseña no coincide con la cuenta existente para este correo.");
    }

    @Test
    void registerCustomerRejectsInactiveStoreAndExistingAccountFromAnotherRole() {
        Store inactive = store();
        inactive.setStoreStatus(StoreStatus.INACTIVE);
        when(storeRepository.findBySlug("inactiva")).thenReturn(Optional.of(inactive));

        assertThatThrownBy(() -> service.registerCustomer(customerDto("cliente@test.com", "Password123!"), "inactiva"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Store not found or inactive");

        Store store = store();
        UserAccount merchantAccount = account(70, "merchant-owner@test.com", new PasswordHashService().hash("Password123!"));
        when(storeRepository.findBySlug("tienda")).thenReturn(Optional.of(store));
        when(userAccountRepository.findByEmail("merchant-owner@test.com")).thenReturn(Optional.of(merchantAccount));
        when(customerRepository.existsByUserAccountId(70)).thenReturn(false);

        assertThatThrownBy(() -> service.registerCustomer(customerDto("merchant-owner@test.com", "Password123!"), "tienda"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Este correo pertenece a otro tipo de cuenta.");
    }

    private UserAccount account(Integer id, String email, String password) {
        UserAccount account = new UserAccount();
        account.setId(id);
        account.setEmail(email);
        account.setPassword(password);
        account.setActive(true);
        return account;
    }

    private LoginRequestDTO login(String email, String password) {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private CreateUserDTO createUser(Role role, String password) {
        CreateUserDTO dto = new CreateUserDTO();
        dto.setEmail("admin@test.com");
        dto.setPassword(password);
        dto.setRole(role);
        dto.setDocumentType(DocumentType.DNI);
        dto.setDocumentNumber("12345678");
        dto.setFirstName("Ana");
        dto.setPaternalSurname("Perez");
        dto.setMaternalSurname("Rojas");
        dto.setBirthDate(LocalDate.of(1990, 1, 1));
        dto.setPhone("999999999");
        dto.setGender(Gender.FEMALE);
        return dto;
    }

    private RegisterCustomerDTO customerDto(String email, String password) {
        RegisterCustomerDTO dto = new RegisterCustomerDTO();
        dto.setEmail(email);
        dto.setPassword(password);
        dto.setDocumentType(DocumentType.DNI);
        dto.setDocumentNumber("12345678");
        dto.setFirstName("Luis");
        dto.setPaternalSurname("Perez");
        dto.setMaternalSurname("Rojas");
        dto.setBirthDate(LocalDate.of(1995, 2, 2));
        dto.setPhone("988777666");
        dto.setGender(Gender.MALE);
        return dto;
    }

    private Store store() {
        Store store = new Store();
        store.setId(5);
        store.setSlug("tienda");
        store.setStoreStatus(StoreStatus.ACTIVE);
        store.setActive(true);
        return store;
    }
}
