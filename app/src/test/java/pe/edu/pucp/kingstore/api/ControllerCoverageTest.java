package pe.edu.pucp.kingstore.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.kingstore.api.controller.public_.PublicStoreController;
import pe.edu.pucp.kingstore.api.controller.admin.AuditController;
import pe.edu.pucp.kingstore.api.controller.customer.AuthController;
import pe.edu.pucp.kingstore.api.controller.admin.BulkUploadController;
import pe.edu.pucp.kingstore.api.controller.customer.CustomerAuthController;
import pe.edu.pucp.kingstore.api.controller.admin.StoreCategoryController;
import pe.edu.pucp.kingstore.api.controller.admin.StoreController;
import pe.edu.pucp.kingstore.api.controller.admin.UserController;
import pe.edu.pucp.kingstore.domain.dto.bulk.BulkUploadResponseDTO;
import pe.edu.pucp.kingstore.domain.dto.store.StoreCategoryDTO;
import pe.edu.pucp.kingstore.domain.dto.store.StoreDTO;
import pe.edu.pucp.kingstore.domain.dto.store.StorePublicDTO;
import pe.edu.pucp.kingstore.domain.dto.user.CreateUserDTO;
import pe.edu.pucp.kingstore.domain.dto.user.CustomerProfileDTO;
import pe.edu.pucp.kingstore.domain.dto.user.LoginRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.user.LoginResponseDTO;
import pe.edu.pucp.kingstore.domain.dto.user.MerchantResponseDTO;
import pe.edu.pucp.kingstore.domain.dto.user.RegisterCustomerDTO;
import pe.edu.pucp.kingstore.domain.dto.user.*;
import pe.edu.pucp.kingstore.domain.model.audit.AuditLog;
import pe.edu.pucp.kingstore.domain.model.audit.enums.AuditLevel;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.StoreCategory;
import pe.edu.pucp.kingstore.domain.model.store.enums.PrimaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.SecondaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.domain.model.store.enums.TertiaryColor;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.domain.model.user.SystemAdministrator;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
import pe.edu.pucp.kingstore.domain.model.user.enums.DocumentType;
import pe.edu.pucp.kingstore.domain.model.user.enums.Gender;
import pe.edu.pucp.kingstore.domain.model.user.enums.Role;
import pe.edu.pucp.kingstore.repository.store.StoreCategoryRepository;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.CustomerRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.repository.user.SystemAdministratorRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;
import pe.edu.pucp.kingstore.service.audit.AuditLogService;
import pe.edu.pucp.kingstore.service.bulk.BulkUploadService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.security.JwtUtil;
import pe.edu.pucp.kingstore.service.store.StoreCategoryService;
import pe.edu.pucp.kingstore.service.store.StoreService;
import pe.edu.pucp.kingstore.service.user.UserAccountService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ControllerCoverageTest {

    private final UserAccountService userAccountService = mock(UserAccountService.class);
    private final UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
    private final MerchantRepository merchantRepository = mock(MerchantRepository.class);
    private final CustomerRepository customerRepository = mock(CustomerRepository.class);
    private final SystemAdministratorRepository adminRepository = mock(SystemAdministratorRepository.class);
    private final StoreRepository storeRepository = mock(StoreRepository.class);

    @Test
    void userControllerBuildsResponsesForRolesAndHandlesErrors() {
        UserController controller = new UserController(
                userAccountService,
                userAccountRepository,
                merchantRepository,
                customerRepository,
                adminRepository,
                storeRepository
        );

        UserAccount merchantAccount = account(1, "merchant@test.com");
        Merchant merchant = merchant(10, merchantAccount);
        Store merchantStore = store(100, "Merchant Store", merchant);

        UserAccount customerAccount = account(2, "customer@test.com");
        Customer customer = customer(20, customerAccount, store(200, "Customer Store", null));

        UserAccount adminAccount = account(3, "admin@test.com");
        SystemAdministrator admin = admin(30, adminAccount);

        UserAccount unknownAccount = account(4, "unknown@test.com");

        when(userAccountRepository.findAll()).thenReturn(
                List.of(merchantAccount, customerAccount, adminAccount, unknownAccount)
        );
        when(merchantRepository.findAll()).thenReturn(List.of(merchant));
        when(customerRepository.findAll()).thenReturn(List.of(customer));
        when(adminRepository.findAll()).thenReturn(List.of(admin));
        when(storeRepository.findAll()).thenReturn(List.of(merchantStore));

        ResponseEntity<List<UserResponseDTO>> all = controller.findAll(null);
        assertThat(all.getBody()).extracting(UserResponseDTO::getRole)
                .containsExactly("MERCHANT", "CUSTOMER", "SYSTEM_ADMIN", "UNKNOWN");
        assertThat(all.getBody().get(0).getStoreName()).isEqualTo("Merchant Store");

        ResponseEntity<List<UserResponseDTO>> filtered = controller.findAll("MERCHANT");
        assertThat(filtered.getBody()).hasSize(1);

        when(userAccountService.getById(1)).thenReturn(merchantAccount);
        assertThat(controller.getById(1).getStatusCode()).isEqualTo(HttpStatus.OK);
        when(userAccountService.getById(99)).thenThrow(new ResourceNotFoundException("User", 99));
        assertThat(controller.getById(99).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        when(userAccountService.createWithRole(any())).thenReturn(adminAccount);
        assertThat(controller.create(new CreateUserDTO()).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        when(userAccountService.createWithRole(any())).thenThrow(new BusinessRuleException("bad"));
        assertThat(controller.create(new CreateUserDTO()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        when(userAccountService.updateUser(eq(3), any())).thenReturn(adminAccount);
        assertThat(controller.update(3, new CreateUserDTO()).getStatusCode()).isEqualTo(HttpStatus.OK);
        when(userAccountService.updateUser(eq(98), any())).thenThrow(new ResourceNotFoundException("User", 98));
        assertThat(controller.update(98, new CreateUserDTO()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        when(userAccountService.updateUser(eq(97), any())).thenThrow(new BusinessRuleException("bad"));
        assertThat(controller.update(97, new CreateUserDTO()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        when(userAccountService.deactivate(3)).thenReturn(adminAccount);
        assertThat(controller.deactivate(3).getStatusCode()).isEqualTo(HttpStatus.OK);
        when(userAccountService.deactivate(98)).thenThrow(new ResourceNotFoundException("User", 98));
        assertThat(controller.deactivate(98).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        when(userAccountService.reactivate(97)).thenThrow(new BusinessRuleException("bad"));
        assertThat(controller.reactivate(97).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        when(merchantRepository.findAll()).thenReturn(List.of(merchant));
        ResponseEntity<List<MerchantResponseDTO>> merchants = controller.findMerchants("merchant");
        assertThat(merchants.getBody()).singleElement().extracting(MerchantResponseDTO::getEmail)
                .isEqualTo("merchant@test.com");
    }

    @Test
    void storeControllerDelegatesAndMapsValidationResponses() {
        StoreService storeService = mock(StoreService.class);
        StoreCategoryRepository categoryRepository = mock(StoreCategoryRepository.class);
        StoreController controller = new StoreController(storeService);
        Store store = store(1, "Original", merchant(11, account(11, "owner@test.com")));
        StoreDTO dto = new StoreDTO();
        dto.setStoreName("Updated");
        dto.setSlug("updated");
        dto.setDescription("new");
        dto.setPrimaryColor(PrimaryColor.MIDNIGHT);
        dto.setSecondaryColor(SecondaryColor.SAGE);
        dto.setTertiaryColor(TertiaryColor.RAW_GOLD);
        dto.setMerchantId(11);
        dto.setCategoryId(2);
        StoreCategory category = new StoreCategory();
        category.setId(2);

        when(storeService.findStores("q", StoreStatus.ACTIVE)).thenReturn(List.of(store));
        assertThat(controller.findStores("q", StoreStatus.ACTIVE).getBody()).containsExactly(store);
        when(storeService.findStores("bad", null)).thenThrow(new BusinessRuleException("bad"));
        assertThat(controller.findStores("bad", null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        when(storeService.getById(1)).thenReturn(store);
        assertThat(controller.getById(1).getStatusCode()).isEqualTo(HttpStatus.OK);
        when(storeService.createFromDTO(dto)).thenReturn(store);
        assertThat(controller.create(dto).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        when(storeService.createFromDTO(new StoreDTO())).thenThrow(new BusinessRuleException("bad"));
        assertThat(controller.create(new StoreDTO()).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        store.setStoreName("Updated");
        when(storeService.updateFromDTO(1, dto)).thenReturn(store);
        ResponseEntity<?> updated = controller.update(1, dto);
        assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updated.getBody()).isEqualTo(store);

        when(storeService.updateFromDTO(404, dto)).thenThrow(new ResourceNotFoundException("Store", 404));
        assertThat(controller.update(404, dto).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        when(storeService.suspend(1)).thenReturn(store);
        when(storeService.deactivate(1)).thenReturn(store);
        when(storeService.reactivate(1)).thenReturn(store);
        assertThat(controller.suspend(1).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.deactivate(1).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.reactivate(1).getStatusCode()).isEqualTo(HttpStatus.OK);
        when(storeService.suspend(2)).thenThrow(new BusinessRuleException("bad"));
        assertThat(controller.suspend(2).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Map<String, Object> metrics = Map.of("active", 1L);
        when(storeService.getMetrics()).thenReturn(metrics);
        assertThat(controller.getMetrics().getBody()).isEqualTo(metrics);
        when(storeService.getMetrics()).thenThrow(new BusinessRuleException("bad"));
        assertThat(controller.getMetrics().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void publicStoreControllerExposesActiveStoresOnly() {
        StoreService storeService = mock(StoreService.class);
        PublicStoreController controller = new PublicStoreController(storeService);

        Store activeStore = store(1, "Ripley", null);
        StoreCategory category = new StoreCategory();
        category.setId(1);
        category.setStoreCategoryName("Moda");
        activeStore.setCategory(category);

        StorePublicDTO publicDTO = new StorePublicDTO();
        publicDTO.setSlug("ripley");
        publicDTO.setStoreName("Ripley");
        publicDTO.setCategory("Moda");
        when(storeService.findPublicStores()).thenReturn(List.of(activeStore));
        when(storeService.toPublicDTO(activeStore)).thenReturn(publicDTO);
        @SuppressWarnings("unchecked") List<StorePublicDTO> result = (List<StorePublicDTO>) controller.findPublicStores().getBody();
        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.getSlug()).isEqualTo("ripley");
            assertThat(item.getCategory()).isEqualTo("Moda");
            assertThat(item.getStoreName()).isEqualTo("Ripley");
        });

        when(storeService.findPublicBySlug("ripley")).thenReturn(Optional.of(activeStore));
        ResponseEntity<?> found = controller.getPublicStore("ripley");
        assertThat(found.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((StorePublicDTO) found.getBody()).getSlug()).isEqualTo("ripley");

        when(storeService.findPublicBySlug("missing")).thenReturn(Optional.empty());
        assertThat(controller.getPublicStore("missing").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void categoryAuditAndBulkControllersReturnExpectedBodies() throws IOException {
        StoreCategoryService categoryService = mock(StoreCategoryService.class);
        StoreCategoryController categoryController = new StoreCategoryController(categoryService);
        StoreCategory category = new StoreCategory();
        category.setId(1);
        category.setStoreCategoryName("Ropa");
        StoreCategoryDTO categoryDTO = new StoreCategoryDTO();
        categoryDTO.setStoreCategoryName("Ropa");

        when(categoryService.search("ro")).thenReturn(List.of(category));
        assertThat(categoryController.findAll("ro").getBody()).containsExactly(category);
        when(categoryService.getById(1)).thenReturn(category);
        assertThat(categoryController.getById(1).getStatusCode()).isEqualTo(HttpStatus.OK);
        when(categoryService.getById(9)).thenThrow(new ResourceNotFoundException("Category", 9));
        assertThat(categoryController.getById(9).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        when(categoryService.createFromDTO(categoryDTO)).thenReturn(category);
        assertThat(categoryController.create(categoryDTO).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        when(categoryService.updateFromDTO(1, categoryDTO)).thenReturn(category);
        assertThat(categoryController.update(1, categoryDTO).getStatusCode()).isEqualTo(HttpStatus.OK);
        doNothing().when(categoryService).delete(1);
        when(categoryService.deactivate(1)).thenReturn(category);
        when(categoryService.reactivate(1)).thenReturn(category);
        assertThat(categoryController.delete(1).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(categoryController.deactivate(1).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(categoryController.reactivate(1).getStatusCode()).isEqualTo(HttpStatus.OK);
        doThrow(new BusinessRuleException("bad")).when(categoryService).delete(2);
        assertThat(categoryController.delete(2).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        AuditLogService auditLogService = mock(AuditLogService.class);
        AuditController auditController = new AuditController(auditLogService);
        AuditLog log = new AuditLog();
        when(auditLogService.findAll(AuditLevel.INFO, "u@test.com", "tenant", "today")).thenReturn(List.of(log));
        assertThat(auditController.findAll(AuditLevel.INFO, "u@test.com", "tenant", "today").getBody()).containsExactly(log);
        when(auditLogService.findAll(null, null, null, "bad")).thenThrow(new BusinessRuleException("bad"));
        assertThat(auditController.findAll(null, null, null, "bad").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        BulkUploadService bulkUploadService = mock(BulkUploadService.class);
        BulkUploadController bulkController = new BulkUploadController(
                bulkUploadService,
                userAccountRepository,
                storeRepository
        );
        assertThat(bulkController.upload(null, null, null).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        MultipartFile csv = new MockMultipartFile("merchants", "m.csv", "text/csv", "email\nx@test.com".getBytes());
        BulkUploadResponseDTO uploadResponse = new BulkUploadResponseDTO();
        when(bulkUploadService.process(csv, null, null)).thenReturn(uploadResponse);
        assertThat(bulkController.upload(csv, null, null).getBody()).isSameAs(uploadResponse);
        MultipartFile failing = mock(MultipartFile.class);
        when(failing.isEmpty()).thenReturn(false);
        when(bulkUploadService.process(failing, null, null)).thenThrow(new IOException("disk"));
        assertThat(bulkController.upload(failing, null, null).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

        when(userAccountRepository.findAll()).thenReturn(List.of(account(1, "EMAIL@Test.COM")));
        assertThat(bulkController.existingEmails().getBody()).containsExactly("email@test.com");
        when(storeRepository.findAll()).thenReturn(List.of(store(1, "Main Store", null)));
        assertThat(bulkController.existingStores().getBody().get("storeNames")).containsExactly("main store");
        assertThat(bulkController.templateMerchants().getHeaders().getFirst("Content-Disposition"))
                .contains("plantilla_comerciantes.csv");
        assertThat(bulkController.templateStores().getHeaders().getFirst("Content-Disposition"))
                .contains("plantilla_tiendas.csv");
    }

    @Test
    void authControllersHandleAdminMerchantCustomerAndFailures() {
        JwtUtil jwtUtil = mock(JwtUtil.class);
        StoreService storeService = mock(StoreService.class);
        AuthController authController = new AuthController(userAccountService, jwtUtil, storeService);

        LoginRequestDTO request = new LoginRequestDTO();
        LoginResponseDTO adminLogin = login(1, "admin@test.com", Role.SYSTEM_ADMIN);
        when(userAccountService.authenticate(request)).thenReturn(adminLogin);
        when(jwtUtil.generateToken(1, "admin@test.com", Role.SYSTEM_ADMIN, null)).thenReturn("admin-token");
        assertThat(((LoginResponseDTO) authController.login(request).getBody()).getToken()).isEqualTo("admin-token");

        LoginRequestDTO merchantRequest = new LoginRequestDTO();
        LoginResponseDTO merchantLogin = login(2, "merchant@test.com", Role.MERCHANT);
        when(userAccountService.authenticate(merchantRequest)).thenReturn(merchantLogin);
        when(storeService.findLoginSlugByUserAccountId(2)).thenReturn(Optional.of("merchant-store"));
        when(jwtUtil.generateToken(2, "merchant@test.com", Role.MERCHANT, "merchant-store")).thenReturn("merchant-token");
        LoginResponseDTO merchantBody = (LoginResponseDTO) authController.login(merchantRequest).getBody();
        assertThat(merchantBody.getToken()).isEqualTo("merchant-token");
        assertThat(merchantBody.getStoreSlug()).isEqualTo("merchant-store");

        LoginRequestDTO customerRequest = new LoginRequestDTO();
        when(userAccountService.authenticate(customerRequest)).thenReturn(login(3, "customer@test.com", Role.CUSTOMER));
        assertThat(authController.login(customerRequest).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

        LoginRequestDTO failingRequest = new LoginRequestDTO();
        when(userAccountService.authenticate(failingRequest)).thenThrow(new BusinessRuleException("bad credentials"));
        assertThat(authController.login(failingRequest).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        CustomerAuthController customerController = new CustomerAuthController(userAccountService, jwtUtil);

//        RegisterCustomerDTO createUserDTO = new RegisterCustomerDTO();
//        RegisterCustomerDTO badCreate = new RegisterCustomerDTO();
//
//        assertThat(customerController.register("store", createUserDTO).getStatusCode()).isEqualTo(HttpStatus.CREATED);
//        assertThat(createUserDTO.getRole()).isEqualTo(Role.CUSTOMER);
//
//        doThrow(new BusinessRuleException("bad")).when(userAccountService).createWithRole(badCreate, "store");
//        assertThat(customerController.register("store", badCreate).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

       // CustomerAuthController customerController = new CustomerAuthController(userAccountService, jwtUtil);
        RegisterCustomerDTO createUserDTO = new RegisterCustomerDTO();

        when(userAccountService.registerCustomer(createUserDTO, "store")).thenReturn(account(5, "new@test.com"));
        assertThat(customerController.register("store", createUserDTO).getStatusCode()).isEqualTo(HttpStatus.CREATED);

        RegisterCustomerDTO badCreate = new RegisterCustomerDTO();
        doThrow(new BusinessRuleException("bad")).when(userAccountService).registerCustomer(badCreate, "store");
        assertThat(customerController.register("store", badCreate).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);


//        LoginRequestDTO customerStoreRequest = new LoginRequestDTO();
//        LoginResponseDTO customerLogin = login(4, "store-customer@test.com", Role.CUSTOMER);
//        when(userAccountService.authenticateCustomer("store", customerStoreRequest)).thenReturn(customerLogin);
//        when(jwtUtil.generateToken(4, "store-customer@test.com", Role.CUSTOMER, "store")).thenReturn("customer-token");
//        assertThat(((LoginResponseDTO) customerController.login("store", customerStoreRequest).getBody()).getToken())
//                .isEqualTo("customer-token");
        LoginRequestDTO customerStoreRequest = new LoginRequestDTO();
        LoginResponseDTO customerLogin = login(4, "store-customer@test.com", Role.CUSTOMER);
        customerLogin.setStoreSlug("store"); // ← agregar esto
        when(userAccountService.authenticateCustomer("store", customerStoreRequest)).thenReturn(customerLogin);
        when(jwtUtil.generateToken(4, "store-customer@test.com", Role.CUSTOMER, "store")).thenReturn("customer-token");
        assertThat(((LoginResponseDTO) customerController.login("store", customerStoreRequest).getBody()).getToken())
                .isEqualTo("customer-token");

        LoginRequestDTO badCustomerRequest = new LoginRequestDTO();
        when(userAccountService.authenticateCustomer("store", badCustomerRequest)).thenThrow(new BusinessRuleException("bad"));
        assertThat(customerController.login("store", badCustomerRequest).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        // Login con el slug de otra tienda: la validacion cliente-tienda debe rechazarlo
        LoginRequestDTO mismatchRequest = new LoginRequestDTO();
        when(userAccountService.authenticateCustomer("other-store", mismatchRequest))
                .thenThrow(new BusinessRuleException("Customer does not belong to this store"));
        assertThat(customerController.login("other-store", mismatchRequest).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        // GET /stores/{slug}/customers/me
        CustomerProfileDTO profile = new CustomerProfileDTO();
        profile.setId(4);
        profile.setFirstName("Ana");
        profile.setLastName("Lopez Perez");
        profile.setEmail("store-customer@test.com");
        profile.setRole(Role.CUSTOMER);
        profile.setStoreSlug("store");
        when(userAccountService.getCustomerProfile(4, "store")).thenReturn(profile);
        Authentication customerAuth = new UsernamePasswordAuthenticationToken("4", null, List.of());
        ResponseEntity<?> meResponse = customerController.me("store", customerAuth);
        assertThat(meResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((CustomerProfileDTO) meResponse.getBody()).getStoreSlug()).isEqualTo("store");

        // /customers/me con token de otra tienda (validacion delegada al service)
        when(userAccountService.getCustomerProfile(4, "other-store"))
                .thenThrow(new BusinessRuleException("Customer does not belong to this store"));
        assertThat(customerController.me("other-store", customerAuth).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);

        // /customers/me sin autenticacion valida
        assertThat(customerController.me("store", null).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminControllersMapRemainingNotFoundAndBusinessRuleBranches() {
        StoreCategoryService categoryService = mock(StoreCategoryService.class);
        StoreCategoryController categoryController = new StoreCategoryController(categoryService);
        StoreCategoryDTO categoryDTO = new StoreCategoryDTO();
        categoryDTO.setStoreCategoryName("Calzado");

        when(categoryService.createFromDTO(categoryDTO)).thenThrow(new BusinessRuleException("bad category"));
        assertThat(categoryController.create(categoryDTO).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        when(categoryService.updateFromDTO(404, categoryDTO)).thenThrow(new ResourceNotFoundException("Category", 404));
        assertThat(categoryController.update(404, categoryDTO).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        when(categoryService.updateFromDTO(2, categoryDTO)).thenThrow(new BusinessRuleException("bad update"));
        assertThat(categoryController.update(2, categoryDTO).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        doThrow(new ResourceNotFoundException("Category", 404)).when(categoryService).delete(404);
        assertThat(categoryController.delete(404).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        when(categoryService.deactivate(404)).thenThrow(new ResourceNotFoundException("Category", 404));
        assertThat(categoryController.deactivate(404).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        when(categoryService.reactivate(404)).thenThrow(new ResourceNotFoundException("Category", 404));
        assertThat(categoryController.reactivate(404).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        StoreService storeService = mock(StoreService.class);
        StoreCategoryRepository categoryRepository = mock(StoreCategoryRepository.class);
        StoreController storeController = new StoreController(storeService);
        StoreDTO storeDTO = new StoreDTO();
        storeDTO.setStoreName("Updated");
        Store store = store(7, "Store", merchant(7, account(7, "merchant7@test.com")));

        when(storeService.getById(404)).thenThrow(new ResourceNotFoundException("Store", 404));
        assertThat(storeController.getById(404).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        when(storeService.updateFromDTO(7, storeDTO)).thenThrow(new BusinessRuleException("bad store"));
        assertThat(storeController.update(7, storeDTO).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        when(storeService.suspend(404)).thenThrow(new ResourceNotFoundException("Store", 404));
        assertThat(storeController.suspend(404).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        when(storeService.deactivate(404)).thenThrow(new ResourceNotFoundException("Store", 404));
        assertThat(storeController.deactivate(404).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        when(storeService.deactivate(405)).thenThrow(new BusinessRuleException("bad deactivate"));
        assertThat(storeController.deactivate(405).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        when(storeService.reactivate(404)).thenThrow(new ResourceNotFoundException("Store", 404));
        assertThat(storeController.reactivate(404).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        when(storeService.reactivate(405)).thenThrow(new BusinessRuleException("bad reactivate"));
        assertThat(storeController.reactivate(405).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    private static UserAccount account(Integer id, String email) {
        UserAccount account = new UserAccount();
        account.setId(id);
        account.setEmail(email);
        account.setPassword("secret");
        account.setActive(true);
        return account;
    }

    private static Merchant merchant(Integer id, UserAccount account) {
        Merchant merchant = new Merchant();
        merchant.setId(id);
        merchant.setUserAccount(account);
        merchant.setFirstName("Ana");
        merchant.setPaternalSurname("Perez");
        merchant.setMaternalSurname("Lopez");
        merchant.setDocumentNumber("12345678");
        merchant.setDocumentType(DocumentType.DNI);
        merchant.setBirthDate(LocalDate.of(1990, 1, 1));
        merchant.setPhone("999999999");
        merchant.setGender(Gender.FEMALE);
        merchant.setRuc("20123456789");
        return merchant;
    }

    private static Customer customer(Integer id, UserAccount account, Store store) {
        Customer customer = new Customer();
        customer.setId(id);
        customer.setUserAccount(account);
        customer.setStore(store);
        customer.setFirstName("Luis");
        customer.setPaternalSurname("Rojas");
        customer.setMaternalSurname("Diaz");
        customer.setDocumentNumber("87654321");
        customer.setDocumentType(DocumentType.DNI);
        customer.setBirthDate(LocalDate.of(1992, 2, 2));
        customer.setPhone("988888888");
        customer.setGender(Gender.MALE);
        return customer;
    }

    private static SystemAdministrator admin(Integer id, UserAccount account) {
        SystemAdministrator admin = new SystemAdministrator();
        admin.setId(id);
        admin.setUserAccount(account);
        admin.setFirstName("Root");
        admin.setPaternalSurname("Admin");
        admin.setMaternalSurname("System");
        admin.setDocumentNumber("11111111");
        admin.setDocumentType(DocumentType.DNI);
        admin.setBirthDate(LocalDate.of(1985, 3, 3));
        admin.setPhone("977777777");
        admin.setGender(Gender.NOT_SPECIFIED);
        admin.setPosition("Lead");
        return admin;
    }

    private static Store store(Integer id, String name, Merchant merchant) {
        Store store = new Store();
        store.setId(id);
        store.setStoreName(name);
        store.setSlug(name.toLowerCase().replace(" ", "-"));
        store.setDescription("description");
        store.setPrimaryColor(PrimaryColor.ONYX_BLACK);
        store.setSecondaryColor(SecondaryColor.SLATE);
        store.setTertiaryColor(TertiaryColor.RAW_GOLD);
        store.setStoreStatus(StoreStatus.ACTIVE);
        store.setMerchant(merchant);
        return store;
    }

    private static LoginResponseDTO login(Integer id, String email, Role role) {
        LoginResponseDTO response = new LoginResponseDTO();
        response.setId(id);
        response.setEmail(email);
        response.setRole(role);
        return response;
    }
}
