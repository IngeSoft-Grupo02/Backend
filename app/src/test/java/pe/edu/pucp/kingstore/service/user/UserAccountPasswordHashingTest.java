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
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
import pe.edu.pucp.kingstore.domain.model.user.enums.DocumentType;
import pe.edu.pucp.kingstore.domain.model.user.enums.Gender;
import pe.edu.pucp.kingstore.domain.model.user.enums.Role;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.CustomerRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.repository.user.SystemAdministratorRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
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

    private UserAccount account(Integer id, String email, String password) {
        UserAccount account = new UserAccount();
        account.setId(id);
        account.setEmail(email);
        account.setPassword(password);
        account.setActive(true);
        return account;
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
