package pe.edu.pucp.kingstore.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import pe.edu.pucp.kingstore.domain.dto.store.StoreCategoryDTO;
import pe.edu.pucp.kingstore.domain.dto.store.StoreDTO;
import pe.edu.pucp.kingstore.domain.dto.user.CreateUserDTO;
import pe.edu.pucp.kingstore.domain.dto.user.CustomerProfileDTO;
import pe.edu.pucp.kingstore.domain.dto.user.LoginRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.user.LoginResponseDTO;
import pe.edu.pucp.kingstore.domain.dto.user.RegisterCustomerDTO;
import pe.edu.pucp.kingstore.domain.model.audit.AuditLog;
import pe.edu.pucp.kingstore.domain.model.audit.enums.AuditLevel;
import pe.edu.pucp.kingstore.domain.model.cart.CartItem;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.order.OrderItem;
import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;
import pe.edu.pucp.kingstore.domain.model.payment.PaymentReceipt;
import pe.edu.pucp.kingstore.domain.model.payment.enums.PaymentMethod;
import pe.edu.pucp.kingstore.domain.model.product.Discount;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import pe.edu.pucp.kingstore.domain.model.product.enums.VolumeType;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.QuotationItem;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
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
import pe.edu.pucp.kingstore.repository.audit.AuditLogRepository;
import pe.edu.pucp.kingstore.repository.cart.ShoppingCartRepository;
import pe.edu.pucp.kingstore.repository.order.OrderRepository;
import pe.edu.pucp.kingstore.repository.payment.PaymentReceiptRepository;
import pe.edu.pucp.kingstore.repository.product.DiscountRepository;
import pe.edu.pucp.kingstore.repository.product.ProductRepository;
import pe.edu.pucp.kingstore.repository.product.ProductVariantRepository;
import pe.edu.pucp.kingstore.repository.quotation.QuotationRepository;
import pe.edu.pucp.kingstore.repository.store.StoreCategoryRepository;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.CustomerRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.repository.user.SystemAdministratorRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;
import pe.edu.pucp.kingstore.service.audit.AuditLogService;
import pe.edu.pucp.kingstore.service.cart.ShoppingCartService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.order.OrderService;
import pe.edu.pucp.kingstore.service.payment.PaymentReceiptService;
import pe.edu.pucp.kingstore.service.product.DiscountService;
import pe.edu.pucp.kingstore.service.product.ProductService;
import pe.edu.pucp.kingstore.service.product.ProductVariantService;
import pe.edu.pucp.kingstore.service.quotation.QuotationService;
import pe.edu.pucp.kingstore.service.security.JwtUtil;
import pe.edu.pucp.kingstore.service.storage.LocalStorageService;
import pe.edu.pucp.kingstore.service.storage.S3StorageService;
import pe.edu.pucp.kingstore.service.store.StoreCategoryService;
import pe.edu.pucp.kingstore.service.store.StoreService;
import pe.edu.pucp.kingstore.service.user.CustomerService;
import pe.edu.pucp.kingstore.service.user.MerchantService;
import pe.edu.pucp.kingstore.service.user.SystemAdministratorService;
import pe.edu.pucp.kingstore.service.user.UserAccountService;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CoreServiceCoverageTest {

    @Mock ProductRepository productRepository;
    @Mock ProductVariantRepository productVariantRepository;
    @Mock DiscountRepository discountRepository;
    @Mock ShoppingCartRepository shoppingCartRepository;
    @Mock QuotationRepository quotationRepository;
    @Mock OrderRepository orderRepository;
    @Mock PaymentReceiptRepository paymentReceiptRepository;
    @Mock StoreRepository storeRepository;
    @Mock StoreCategoryRepository categoryRepository;
    @Mock MerchantRepository merchantRepository;
    @Mock CustomerRepository customerRepository;
    @Mock SystemAdministratorRepository administratorRepository;
    @Mock UserAccountRepository userAccountRepository;
    @Mock AuditLogRepository auditLogRepository;

    @Test
    void crudServicesCoverCreateUpdateQueriesAndValidation() {
        ProductService productService = new ProductService(productRepository);
        Product product = product();
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(productRepository.findById(5)).thenReturn(Optional.of(product));
        when(productRepository.existsById(5)).thenReturn(true);
        when(productRepository.findAll()).thenReturn(List.of(product));
        when(productRepository.findByStoreId(2)).thenReturn(List.of(product));
        when(productRepository.findByStoreIdAndActive(2, true)).thenReturn(List.of(product));
        when(productRepository.findByNameContainingAndStoreId("shirt", 2)).thenReturn(List.of(product));

        assertThat(productService.create(product).getId()).isNull();
        assertThat(productService.update(5, product).getId()).isEqualTo(5);
        assertThat(productService.findById(5)).contains(product);
        assertThat(productService.getById(5)).isSameAs(product);
        assertThat(productService.findAll()).containsExactly(product);
        assertThat(productService.findActive()).containsExactly(product);
        assertThat(productService.findByStore(2)).containsExactly(product);
        assertThat(productService.findActiveByStore(2)).containsExactly(product);
        assertThat(productService.searchByNameInStore(" shirt ", 2)).containsExactly(product);
        assertThat(productService.deactivate(5).getActive()).isFalse();
        assertThat(productService.reactivate(5).getActive()).isTrue();
        productService.delete(5);
        verify(productRepository).deleteById(5);

        assertThatThrownBy(() -> productService.findByStore(0)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> productService.getById(99)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> productService.create(null)).isInstanceOf(BusinessRuleException.class);
        Product invalidPrice = product();
        invalidPrice.setBasePrice(1);
        invalidPrice.setCostPrice(10);
        assertThatThrownBy(() -> productService.create(invalidPrice)).isInstanceOf(BusinessRuleException.class);
        Product invalidStock = product();
        invalidStock.getVariants().get(0).setStock(-1);
        assertThatThrownBy(() -> productService.create(invalidStock)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void commerceServicesCalculateTotalsAndFindRelatedRecords() {
        DiscountService discountService = new DiscountService(discountRepository);
        Discount discount = discount(product());
        when(discountRepository.save(any(Discount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(discountRepository.findByProductId(7)).thenReturn(List.of(discount));
        assertThat(discountService.create(discount).getDiscountPercentage()).isEqualTo(15);
        assertThat(discountService.findByProduct(7)).containsExactly(discount);
        discount.setMinQuantity(5);
        discount.setMaxQuantity(2);
        assertThatThrownBy(() -> discountService.create(discount)).isInstanceOf(BusinessRuleException.class);

        ProductVariantService variantService = new ProductVariantService(productVariantRepository);
        ProductVariant variant = variant();
        when(productVariantRepository.save(any(ProductVariant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(variantService.create(variant).getStock()).isEqualTo(3);
        variant.setStock(-1);
        assertThatThrownBy(() -> variantService.create(variant)).isInstanceOf(BusinessRuleException.class);

        ShoppingCartService cartService = new ShoppingCartService(shoppingCartRepository);
        ShoppingCart cart = cart();
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(shoppingCartRepository.findByCustomerId(10)).thenReturn(Optional.of(cart));
        assertThat(cartService.create(cart).getTotalAmount()).isEqualTo(17);
        assertThat(cartService.findByCustomer(10)).contains(cart);
        cart.getItems().get(0).setQuantity(0);
        assertThatThrownBy(() -> cartService.create(cart)).isInstanceOf(BusinessRuleException.class);

        QuotationService quotationService = new QuotationService(quotationRepository);
        Quotation quotation = quotation();
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(quotationRepository.findById(6)).thenReturn(Optional.of(quotation));
        when(quotationRepository.findByShoppingCartId(8)).thenReturn(Optional.of(quotation));
        when(quotationRepository.findByStatus(QuotationStatus.PENDING)).thenReturn(List.of(quotation));
        assertThat(quotationService.create(quotation).getStatus()).isEqualTo(QuotationStatus.PENDING);
        assertThat(quotationService.findByShoppingCart(8)).contains(quotation);
        assertThat(quotationService.findByStatus(QuotationStatus.PENDING)).containsExactly(quotation);
        assertThat(quotationService.respond(6, QuotationStatus.APPROVED, "ok").getResponseAt()).isNotNull();
        assertThatThrownBy(() -> quotationService.respond(6, QuotationStatus.PENDING, "no"))
                .isInstanceOf(BusinessRuleException.class);

        OrderService orderService = new OrderService(orderRepository);
        Order order = order();
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.findById(9)).thenReturn(Optional.of(order));
        when(orderRepository.findByQuotationId(6)).thenReturn(Optional.of(order));
        when(orderRepository.findByStatus(OrderStatus.IN_PREPARATION)).thenReturn(List.of(order));
        assertThat(orderService.create(order).getFinalTotal()).isEqualTo(17);
        assertThat(orderService.findByQuotation(6)).contains(order);
        assertThat(orderService.findByStatus(OrderStatus.IN_PREPARATION)).containsExactly(order);
        assertThat(orderService.changeStatus(9, OrderStatus.IN_TRANSIT).getStatus()).isEqualTo(OrderStatus.IN_TRANSIT);
        assertThatThrownBy(() -> orderService.changeStatus(9, null)).isInstanceOf(BusinessRuleException.class);

        PaymentReceiptService receiptService = new PaymentReceiptService(paymentReceiptRepository);
        PaymentReceipt receipt = receipt(order);
        when(paymentReceiptRepository.save(any(PaymentReceipt.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentReceiptRepository.findByOrderId(9)).thenReturn(Optional.of(receipt));
        assertThat(receiptService.create(receipt).getTaxes()).isGreaterThan(0);
        assertThat(receiptService.findByOrder(9)).contains(receipt);
        receipt.setFinalTotal(-1.0);
        assertThatThrownBy(() -> receiptService.create(receipt)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void storeServicesCoverCategoriesDtoStatusAndMetrics() {
        StoreCategoryService categoryService = new StoreCategoryService(categoryRepository);
        StoreCategory category = category(1, "Urban");
        StoreCategoryDTO categoryDTO = new StoreCategoryDTO();
        categoryDTO.setStoreCategoryName(" Urban ");
        when(categoryRepository.save(any(StoreCategory.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryRepository.findAll()).thenReturn(List.of(), List.of(category), List.of(category));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        assertThat(categoryService.createFromDTO(categoryDTO).getStoreCategoryName()).isEqualTo("Urban");
        assertThat(categoryService.search("urb")).containsExactly(category);
        assertThat(categoryService.search(null)).containsExactly(category);
        categoryDTO.setStoreCategoryName("Casual");
        assertThat(categoryService.updateFromDTO(1, categoryDTO).getStoreCategoryName()).isEqualTo("Casual");
        StoreCategory duplicate = category(2, "Casual");
        when(categoryRepository.findAll()).thenReturn(List.of(duplicate));
        assertThatThrownBy(() -> categoryService.updateFromDTO(1, categoryDTO))
                .isInstanceOf(BusinessRuleException.class);

        StoreService storeService = new StoreService(storeRepository, merchantRepository, categoryRepository, quotationRepository);
        Store store = store();
        Store suspended = store();
        suspended.setId(3);
        suspended.setSlug("paused");
        suspended.setStoreStatus(StoreStatus.SUSPENDED);
        Merchant merchant = merchantProfile();
        when(storeRepository.save(any(Store.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storeRepository.findById(2)).thenReturn(Optional.of(store));
        when(storeRepository.findBySlug("new-store")).thenReturn(Optional.empty());
        when(storeRepository.findBySlug("store")).thenReturn(Optional.of(store));
        when(storeRepository.findByActive(true)).thenReturn(List.of(store));
        when(storeRepository.findByStoreStatus(StoreStatus.ACTIVE)).thenReturn(List.of(store));
        when(storeRepository.findAllByMerchant_UserAccount_IdAndStoreStatusOrderByIdAsc(22, StoreStatus.ACTIVE))
                .thenReturn(List.of(store));
        when(storeRepository.findAll()).thenReturn(List.of(store, suspended));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(merchantRepository.findById(4)).thenReturn(Optional.of(merchant));

        StoreDTO dto = new StoreDTO();
        dto.setStoreName("New Store");
        dto.setSlug("new-store");
        dto.setCategoryId(1);
        dto.setMerchantId(4);
        dto.setPrimaryColor(PrimaryColor.MIDNIGHT);
        dto.setSecondaryColor(SecondaryColor.SLATE);
        dto.setTertiaryColor(TertiaryColor.COPPER);
        Store created = storeService.createFromDTO(dto);
        assertThat(created.getMerchant()).isSameAs(merchant);
        assertThat(created.getStoreStatus()).isEqualTo(StoreStatus.ACTIVE);
        assertThat(storeService.findBySlug(" Store ")).contains(store);
        assertThat(storeService.findActive()).containsExactly(store);
        assertThat(storeService.findByStatus(StoreStatus.ACTIVE)).containsExactly(store);
        assertThat(storeService.findActiveSlugByUserAccountId(22)).contains("store");
        assertThat(storeService.findStores("pau", null)).containsExactly(suspended);
        assertThat(storeService.getMetrics()).containsEntry("total", 2).containsEntry("active", 1L);
        assertThat(storeService.suspend(2).getStoreStatus()).isEqualTo(StoreStatus.SUSPENDED);
        assertThat(storeService.deactivate(2).getStoreStatus()).isEqualTo(StoreStatus.INACTIVE);
        assertThat(storeService.reactivate(2).getStoreStatus()).isEqualTo(StoreStatus.ACTIVE);
        dto.setCategoryId(null);
        assertThatThrownBy(() -> storeService.createFromDTO(dto)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void userProfileServicesAuthenticateCreateRolesAndAudit() {
        UserAccountService service = new UserAccountService(
                userAccountRepository, customerRepository, merchantRepository, administratorRepository, storeRepository);
        UserAccount account = account(5, "user@kingstore.pe", "secret");
        when(userAccountRepository.findByEmail("user@kingstore.pe")).thenReturn(Optional.of(account));
        when(customerRepository.findByUserAccountId(5)).thenReturn(Optional.of(customerProfile()));
        LoginRequestDTO login = new LoginRequestDTO();
        login.setEmail(" User@Kingstore.pe ");
        login.setPassword("secret");
        LoginResponseDTO response = service.authenticate(login);
        assertThat(response.getRole()).isEqualTo(Role.CUSTOMER);
        assertThatThrownBy(() -> service.authenticate(null)).isInstanceOf(BusinessRuleException.class);

        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount saved = invocation.getArgument(0);
            if (saved.getId() == null) saved.setId(100);
            return saved;
        });
        CreateUserDTO dto = createUserDTO(Role.MERCHANT);
        dto.setRuc("12345678901");
        dto.setStoreId(2);
        when(storeRepository.findById(2)).thenReturn(Optional.of(store()));
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.createWithRole(dto).getId()).isEqualTo(100);

        dto = createUserDTO(Role.SYSTEM_ADMIN);
        when(administratorRepository.save(any(SystemAdministrator.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(service.createWithRole(dto).getEmail()).isEqualTo("person@kingstore.pe");

        when(storeRepository.findBySlug("store")).thenReturn(Optional.of(store()));
        dto = createUserDTO(Role.CUSTOMER);
        assertThat(service.createWithRole(dto, "store").getId()).isEqualTo(100);

        CreateUserDTO update = new CreateUserDTO();
        update.setEmail(" Updated@Kingstore.pe ");
        update.setPassword("new");
        when(userAccountRepository.findById(5)).thenReturn(Optional.of(account));
        assertThat(service.updateUser(5, update).getEmail()).isEqualTo("updated@kingstore.pe");

        CustomerService customerService = new CustomerService(customerRepository);
        MerchantService merchantService = new MerchantService(merchantRepository);
        SystemAdministratorService administratorService = new SystemAdministratorService(administratorRepository);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(administratorRepository.save(any(SystemAdministrator.class))).thenAnswer(invocation -> invocation.getArgument(0));
        assertThat(customerService.create(customerProfile()).getUserAccount()).isNotNull();
        assertThat(merchantService.create(merchantProfile()).getRuc()).isEqualTo("12345678901");
        assertThat(administratorService.create(adminProfile()).getPosition()).isEqualTo("Admin");
        Customer invalid = customerProfile();
        invalid.setFirstName("");
        assertThatThrownBy(() -> customerService.create(invalid)).isInstanceOf(BusinessRuleException.class);

        AuditLogService auditLogService = new AuditLogService(auditLogRepository);
        AuditLog log = new AuditLog();
        auditLogService.save(log);
        verify(auditLogRepository).save(log);
        when(auditLogRepository.findByLevel(AuditLevel.ERROR)).thenReturn(List.of(log));
        when(auditLogRepository.findByUserEmailContainingIgnoreCase("user")).thenReturn(List.of(log));
        when(auditLogRepository.findByTenantSlugContainingIgnoreCase("store")).thenReturn(List.of(log));
        when(auditLogRepository.findAll()).thenReturn(List.of(log));
        assertThat(auditLogService.findAll(AuditLevel.ERROR, null, null, null)).containsExactly(log);
        assertThat(auditLogService.findAll(null, "user", null, null)).containsExactly(log);
        assertThat(auditLogService.findAll(null, null, "store", null)).containsExactly(log);
        assertThat(auditLogService.findAll(null, null, null, null)).containsExactly(log);
        auditLogService.findAll(AuditLevel.INFO, null, null, "TODAY");
        auditLogService.findAll(null, "user", null, "LAST_7_DAYS");
        auditLogService.findAll(null, null, "store", "LAST_30_DAYS");
        auditLogService.findAll(null, null, null, "TODAY");
    }

    @Test
    void customerRegistrationValidatesFieldsBeforeCreatingAccount() {
        UserAccountService service = new UserAccountService(
                userAccountRepository, customerRepository, merchantRepository, administratorRepository, storeRepository);

        when(storeRepository.findBySlug("store")).thenReturn(Optional.of(store()));
        when(storeRepository.findById(2)).thenReturn(Optional.of(store()));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount saved = invocation.getArgument(0);
            if (saved.getId() == null) saved.setId(200);
            return saved;
        });
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegisterCustomerDTO validDni = registerCustomerDTO();
        assertThat(service.registerCustomer(validDni, "store").getId()).isEqualTo(200);

        RegisterCustomerDTO validCe = registerCustomerDTO();
        validCe.setEmail("ce-customer@kingstore.pe");
        validCe.setDocumentType(DocumentType.FOREIGN_ID_CARD);
        validCe.setDocumentNumber("123456789");
        assertThat(service.registerCustomer(validCe, "store").getId()).isEqualTo(200);

        RegisterCustomerDTO validPassport = registerCustomerDTO();
        validPassport.setEmail("passport-customer@kingstore.pe");
        validPassport.setDocumentType(DocumentType.PASSPORT);
        validPassport.setDocumentNumber("123456789");
        assertThat(service.registerCustomer(validPassport, "store").getId()).isEqualTo(200);

        RegisterCustomerDTO dniTooLong = registerCustomerDTO();
        dniTooLong.setDocumentNumber("123456789");
        assertThatThrownBy(() -> service.registerCustomer(dniTooLong, "store"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El DNI debe tener 8 dígitos");

        RegisterCustomerDTO dniWithLetters = registerCustomerDTO();
        dniWithLetters.setDocumentNumber("1234567A");
        assertThatThrownBy(() -> service.registerCustomer(dniWithLetters, "store"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El número de documento solo debe contener dígitos");

        RegisterCustomerDTO phoneTooLong = registerCustomerDTO();
        phoneTooLong.setPhone("1234567890");
        assertThatThrownBy(() -> service.registerCustomer(phoneTooLong, "store"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El celular debe tener 9 dígitos");

        RegisterCustomerDTO phoneWithLetters = registerCustomerDTO();
        phoneWithLetters.setPhone("99999999A");
        assertThatThrownBy(() -> service.registerCustomer(phoneWithLetters, "store"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El celular debe tener 9 dígitos");

        RegisterCustomerDTO invalidEmail = registerCustomerDTO();
        invalidEmail.setEmail("not-an-email");
        assertThatThrownBy(() -> service.registerCustomer(invalidEmail, "store"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Ingresa un correo electrónico válido");

        RegisterCustomerDTO futureBirthDate = registerCustomerDTO();
        futureBirthDate.setBirthDate(LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> service.registerCustomer(futureBirthDate, "store"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La fecha de nacimiento no puede ser una fecha futura");
    }

    @Test
    void jwtAndLocalStorageServicesWork(@TempDir Path tempDir) throws Exception {
        JwtUtil jwtUtil = new JwtUtil();
        String token = jwtUtil.generateToken(7, "user@kingstore.pe", Role.MERCHANT, "store");
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(7);
        assertThat(jwtUtil.extractRole(token)).isEqualTo(Role.MERCHANT);
        assertThat(jwtUtil.extractStoreSlug(token)).isEqualTo("store");
        assertThat(jwtUtil.isTokenValid(token)).isTrue();
        assertThat(jwtUtil.isTokenValid("bad-token")).isFalse();

        LocalStorageService storageService = new LocalStorageService();
        ReflectionTestUtils.setField(storageService, "baseDir", tempDir.toString());
        ReflectionTestUtils.setField(storageService, "baseUrl", "http://localhost/uploads");
        String url = storageService.uploadBytes("logos/store.png", "image".getBytes(), "image/png");
        assertThat(url).isEqualTo("http://localhost/uploads/logos/store.png");
        assertThat(Files.readString(tempDir.resolve("logos/store.png"))).isEqualTo("image");
    }

    @Test
    void s3StorageServiceBuildsPutObjectRequestAndPublicUrl() {
        S3Client s3Client = org.mockito.Mockito.mock(S3Client.class);
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        S3StorageService service = new S3StorageService(s3Client);
        ReflectionTestUtils.setField(service, "bucketName", "kingstore-assets");
        ReflectionTestUtils.setField(service, "region", "us-west-2");

        String url = service.uploadBytes("logos/store.png", "image".getBytes(), "image/png");

        assertThat(url).isEqualTo("https://kingstore-assets.s3.us-west-2.amazonaws.com/logos/store.png");
        ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(captor.capture(), any(RequestBody.class));
        assertThat(captor.getValue().bucket()).isEqualTo("kingstore-assets");
        assertThat(captor.getValue().key()).isEqualTo("logos/store.png");
        assertThat(captor.getValue().contentType()).isEqualTo("image/png");
    }

    @Test
    void userAccountServiceCoversAuthenticationRoleAndCreationFailures() {
        UserAccountService service = new UserAccountService(
                userAccountRepository, customerRepository, merchantRepository, administratorRepository, storeRepository);

        LoginRequestDTO inactiveLogin = login("inactive@kingstore.pe", "secret");
        UserAccount inactiveAccount = account(30, "inactive@kingstore.pe", "secret");
        inactiveAccount.setActive(false);
        when(userAccountRepository.findByEmail("inactive@kingstore.pe")).thenReturn(Optional.of(inactiveAccount));
        assertThatThrownBy(() -> service.authenticate(inactiveLogin)).isInstanceOf(BusinessRuleException.class);

        LoginRequestDTO badPassword = login("bad-password@kingstore.pe", "bad");
        UserAccount badPasswordAccount = account(31, "bad-password@kingstore.pe", "secret");
        when(userAccountRepository.findByEmail("bad-password@kingstore.pe")).thenReturn(Optional.of(badPasswordAccount));
        assertThatThrownBy(() -> service.authenticate(badPassword)).isInstanceOf(BusinessRuleException.class);

        LoginRequestDTO inactiveMerchantLogin = login("inactive-merchant@kingstore.pe", "secret");
        UserAccount inactiveMerchantAccount = account(32, "inactive-merchant@kingstore.pe", "secret");
        Merchant inactiveMerchant = merchantProfile();
        inactiveMerchant.setActive(false);
        when(userAccountRepository.findByEmail("inactive-merchant@kingstore.pe")).thenReturn(Optional.of(inactiveMerchantAccount));
        when(customerRepository.findByUserAccountId(32)).thenReturn(Optional.empty());
        when(merchantRepository.findByUserAccountId(32)).thenReturn(Optional.of(inactiveMerchant));
        assertThatThrownBy(() -> service.authenticate(inactiveMerchantLogin)).isInstanceOf(BusinessRuleException.class);

        LoginRequestDTO adminLogin = login("admin-login@kingstore.pe", "secret");
        UserAccount adminAccount = account(33, "admin-login@kingstore.pe", "secret");
        when(userAccountRepository.findByEmail("admin-login@kingstore.pe")).thenReturn(Optional.of(adminAccount));
        when(customerRepository.findByUserAccountId(33)).thenReturn(Optional.empty());
        when(merchantRepository.findByUserAccountId(33)).thenReturn(Optional.empty());
        when(administratorRepository.findByUserAccountId(33)).thenReturn(Optional.of(adminProfile()));
        assertThat(service.authenticate(adminLogin).getRole()).isEqualTo(Role.SYSTEM_ADMIN);

        LoginRequestDTO noRoleLogin = login("norole@kingstore.pe", "secret");
        UserAccount noRoleAccount = account(34, "norole@kingstore.pe", "secret");
        when(userAccountRepository.findByEmail("norole@kingstore.pe")).thenReturn(Optional.of(noRoleAccount));
        when(customerRepository.findByUserAccountId(34)).thenReturn(Optional.empty());
        when(merchantRepository.findByUserAccountId(34)).thenReturn(Optional.empty());
        when(administratorRepository.findByUserAccountId(34)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.authenticate(noRoleLogin)).isInstanceOf(BusinessRuleException.class);

        when(userAccountRepository.findByEmail("person@kingstore.pe")).thenReturn(Optional.empty());
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> {
            UserAccount saved = invocation.getArgument(0);
            if (saved.getId() == null) saved.setId(200);
            return saved;
        });
        assertThatThrownBy(() -> service.createWithRole(createUserDTO(Role.CUSTOMER)))
                .isInstanceOf(BusinessRuleException.class);

        CreateUserDTO missingCustomerStore = createUserDTO(Role.CUSTOMER);
        missingCustomerStore.setStoreId(404);
        when(storeRepository.findById(404)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createWithRole(missingCustomerStore))
                .isInstanceOf(BusinessRuleException.class);

        CreateUserDTO merchantWithoutRuc = createUserDTO(Role.MERCHANT);
        assertThatThrownBy(() -> service.createWithRole(merchantWithoutRuc))
                .isInstanceOf(BusinessRuleException.class);

        CreateUserDTO merchantMissingStore = createUserDTO(Role.MERCHANT);
        merchantMissingStore.setRuc("12345678901");
        merchantMissingStore.setStoreId(405);
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(storeRepository.findById(405)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createWithRole(merchantMissingStore))
                .isInstanceOf(BusinessRuleException.class);

        Store inactiveStore = store();
        inactiveStore.setStoreStatus(StoreStatus.INACTIVE);
        when(storeRepository.findBySlug("inactive-store")).thenReturn(Optional.of(inactiveStore));
        CreateUserDTO customerBySlug = createUserDTO(Role.CUSTOMER);
        assertThatThrownBy(() -> service.createWithRole(customerBySlug, "inactive-store"))
                .isInstanceOf(BusinessRuleException.class);

        LoginRequestDTO nonCustomerLogin = login("not-customer@kingstore.pe", "secret");
        UserAccount nonCustomer = account(35, "not-customer@kingstore.pe", "secret");
        when(userAccountRepository.findByEmail("not-customer@kingstore.pe")).thenReturn(Optional.of(nonCustomer));
        when(customerRepository.findByUserAccountId(35)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.authenticateCustomer("store", nonCustomerLogin))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void userAccountServiceCoversUpdateDuplicateEmailAndPhoneProfiles() {
        UserAccountService service = new UserAccountService(
                userAccountRepository, customerRepository, merchantRepository, administratorRepository, storeRepository);

        UserAccount phoneAccount = account(40, "phone@kingstore.pe", "secret");
        Merchant merchant = merchantProfile();
        Customer customer = customerProfile();
        SystemAdministrator admin = adminProfile();
        when(userAccountRepository.findById(40)).thenReturn(Optional.of(phoneAccount));
        when(userAccountRepository.findByEmail("phone@kingstore.pe")).thenReturn(Optional.of(phoneAccount));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(merchantRepository.findByUserAccountId(40)).thenReturn(Optional.of(merchant));
        when(customerRepository.findByUserAccountId(40)).thenReturn(Optional.of(customer));
        when(administratorRepository.findByUserAccountId(40)).thenReturn(Optional.of(admin));
        when(merchantRepository.save(any(Merchant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(administratorRepository.save(any(SystemAdministrator.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateUserDTO updatePhone = new CreateUserDTO();
        updatePhone.setPhone("900111222");
        assertThat(service.updateUser(40, updatePhone).getEmail()).isEqualTo("phone@kingstore.pe");
        assertThat(merchant.getPhone()).isEqualTo("900111222");
        assertThat(customer.getPhone()).isEqualTo("900111222");
        assertThat(admin.getPhone()).isEqualTo("900111222");

        UserAccount current = account(41, "current@kingstore.pe", "secret");
        UserAccount duplicate = account(42, "duplicate@kingstore.pe", "secret");
        when(userAccountRepository.findById(41)).thenReturn(Optional.of(current));
        when(userAccountRepository.findByEmail("duplicate@kingstore.pe")).thenReturn(Optional.of(duplicate));
        CreateUserDTO duplicateEmail = new CreateUserDTO();
        duplicateEmail.setEmail(" duplicate@kingstore.pe ");
        assertThatThrownBy(() -> service.updateUser(41, duplicateEmail))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void storeServiceCoversPublicCatalogFiltering() {
        StoreService storeService = new StoreService(storeRepository, merchantRepository, categoryRepository);

        Store activeStore = store();
        Store hiddenStore = store();
        hiddenStore.setId(6);
        hiddenStore.setSlug("hidden");
        hiddenStore.setActive(false);

        when(storeRepository.findByStoreStatus(StoreStatus.ACTIVE)).thenReturn(List.of(activeStore, hiddenStore));
        assertThat(storeService.findPublicStores()).containsExactly(activeStore);

        when(storeRepository.findBySlug("store")).thenReturn(Optional.of(activeStore));
        assertThat(storeService.findPublicBySlug("Store")).contains(activeStore);

        when(storeRepository.findBySlug("hidden")).thenReturn(Optional.of(hiddenStore));
        assertThat(storeService.findPublicBySlug("hidden")).isEmpty();

        Store suspendedStore = store();
        suspendedStore.setId(7);
        suspendedStore.setSlug("suspended-store");
        suspendedStore.setStoreStatus(StoreStatus.SUSPENDED);
        when(storeRepository.findBySlug("suspended-store")).thenReturn(Optional.of(suspendedStore));
        assertThat(storeService.findPublicBySlug("suspended-store")).isEmpty();

        when(storeRepository.findBySlug("missing")).thenReturn(Optional.empty());
        assertThat(storeService.findPublicBySlug("missing")).isEmpty();
    }

    @Test
    void userAccountServiceValidatesCustomerStoreSlugForLoginAndProfile() {
        UserAccountService service = new UserAccountService(
                userAccountRepository, customerRepository, merchantRepository, administratorRepository, storeRepository);

        UserAccount account = account(50, "customer-store@kingstore.pe", "secret");
        Customer customer = customerProfile();
        customer.setId(99);
        when(userAccountRepository.findByEmail("customer-store@kingstore.pe")).thenReturn(Optional.of(account));
        when(customerRepository.findByUserAccountId(50)).thenReturn(Optional.of(customer));

        LoginRequestDTO request = login("customer-store@kingstore.pe", "secret");

        LoginResponseDTO response = service.authenticateCustomer("store", request);
        assertThat(response.getStoreSlug()).isEqualTo("store");
        assertThat(response.getRole()).isEqualTo(Role.CUSTOMER);

        assertThatThrownBy(() -> service.authenticateCustomer("other-store", request))
                .isInstanceOf(BusinessRuleException.class);

        CustomerProfileDTO profile = service.getCustomerProfile(50, "store");
        assertThat(profile.getStoreSlug()).isEqualTo("store");
        assertThat(profile.getLastName()).isEqualTo("Perez Rojas");
        assertThat(profile.getEmail()).isEqualTo("customer@kingstore.pe");

        assertThatThrownBy(() -> service.getCustomerProfile(50, "other-store"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void storeServiceCoversLoginSlugValidationAndEmptyMetricsBranches() {
        StoreService storeService = new StoreService(storeRepository, merchantRepository, categoryRepository, quotationRepository);

        Store suspended = store();
        suspended.setId(50);
        suspended.setSlug("paused");
        suspended.setStoreStatus(StoreStatus.SUSPENDED);
        Store blankSlug = store();
        blankSlug.setId(51);
        blankSlug.setSlug(" ");

        when(storeRepository.findAllByMerchant_UserAccount_Id(22)).thenReturn(List.of(suspended));
        assertThat(storeService.findLoginSlugByUserAccountId(22)).contains("paused");
        when(storeRepository.findAllByMerchant_UserAccount_Id(23)).thenReturn(List.of(blankSlug));
        assertThat(storeService.findLoginSlugByUserAccountId(23)).isEmpty();
        when(storeRepository.findByStoreStatus(StoreStatus.SUSPENDED)).thenReturn(List.of(suspended));
        assertThat(storeService.findStores(null, StoreStatus.SUSPENDED)).containsExactly(suspended);
        when(storeRepository.findAll()).thenReturn(List.of());
        assertThat(storeService.getMetrics()).containsEntry("message", "No stores registered in the platform");

        when(storeRepository.findById(50)).thenReturn(Optional.of(suspended));
        assertThatThrownBy(() -> storeService.suspend(50)).isInstanceOf(BusinessRuleException.class);

        Store duplicateTarget = store();
        duplicateTarget.setId(52);
        duplicateTarget.setSlug("store");
        Store duplicateExisting = store();
        duplicateExisting.setId(53);
        duplicateExisting.setSlug("store");
        when(storeRepository.findBySlug("store")).thenReturn(Optional.of(duplicateExisting));
        assertThatThrownBy(() -> storeService.create(duplicateTarget)).isInstanceOf(BusinessRuleException.class);

        StoreDTO dto = new StoreDTO();
        dto.setStoreName("Missing Category");
        dto.setSlug("missing-category");
        dto.setCategoryId(404);
        when(categoryRepository.findById(404)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> storeService.createFromDTO(dto)).isInstanceOf(BusinessRuleException.class);
    }

    private Product product() {
        Product product = new Product();
        product.setStore(store());
        product.setName("T-shirt");
        product.setCostPrice(10);
        product.setBasePrice(20);
        product.setVariants(List.of(variant()));
        return product;
    }

    private ProductVariant variant() {
        ProductVariant variant = new ProductVariant();
        variant.setId(30);
        variant.setSize("M");
        variant.setColor(Color.RED);
        variant.setStock(3);
        return variant;
    }

    private Discount discount(Product product) {
        Discount discount = new Discount();
        discount.setProduct(product);
        discount.getProduct().setId(7);
        discount.setVolumeType(VolumeType.UNIT);
        discount.setMinQuantity(1);
        discount.setMaxQuantity(10);
        discount.setDiscountPercentage(15);
        return discount;
    }

    private ShoppingCart cart() {
        ShoppingCart cart = new ShoppingCart();
        Customer customer = customerProfile();
        customer.setId(10);
        cart.setCustomer(customer);
        cart.setDiscount(3);
        CartItem item = new CartItem();
        item.setProductVariant(variant());
        item.setQuantity(2);
        item.setPrice(10);
        cart.setItems(List.of(item));
        return cart;
    }

    private Quotation quotation() {
        Quotation quotation = new Quotation();
        ShoppingCart cart = cart();
        cart.setId(8);
        quotation.setShoppingCart(cart);
        quotation.setDiscount(3);
        QuotationItem item = new QuotationItem();
        item.setProductVariant(variant());
        item.setQuantity(2);
        item.setPrice(10);
        quotation.setItems(List.of(item));
        return quotation;
    }

    private Order order() {
        Order order = new Order();
        Quotation quotation = quotation();
        quotation.setId(6);
        order.setQuotation(quotation);
        order.setTotalDiscount(3.0);
        OrderItem item = new OrderItem();
        item.setProductVariant(variant());
        item.setQuantity(2);
        item.setUnitPrice(10.0);
        order.setItems(List.of(item));
        return order;
    }

    private PaymentReceipt receipt(Order order) {
        order.setId(9);
        PaymentReceipt receipt = new PaymentReceipt();
        receipt.setOrder(order);
        receipt.setRuc("12345678901");
        receipt.setPaymentMethod(PaymentMethod.VIRTUAL);
        receipt.setFinalTotal(118.0);
        return receipt;
    }

    private Store store() {
        Store store = new Store();
        store.setId(2);
        store.setStoreName("Store");
        store.setSlug("store");
        store.setCategory(category(1, "Urban"));
        store.setPrimaryColor(PrimaryColor.ONYX_BLACK);
        store.setSecondaryColor(SecondaryColor.SLATE);
        store.setTertiaryColor(TertiaryColor.RAW_GOLD);
        store.setStoreStatus(StoreStatus.ACTIVE);
        return store;
    }

    private StoreCategory category(Integer id, String name) {
        StoreCategory category = new StoreCategory();
        category.setId(id);
        category.setStoreCategoryName(name);
        return category;
    }

    private UserAccount account(Integer id, String email, String password) {
        UserAccount account = new UserAccount();
        account.setId(id);
        account.setEmail(email);
        account.setPassword(password);
        account.setActive(true);
        return account;
    }

    private Customer customerProfile() {
        Customer customer = new Customer();
        customer.setUserAccount(account(11, "customer@kingstore.pe", "secret"));
        customer.setStore(store());
        fillPerson(customer);
        return customer;
    }

    private Merchant merchantProfile() {
        Merchant merchant = new Merchant();
        merchant.setId(4);
        merchant.setUserAccount(account(12, "merchant@kingstore.pe", "secret"));
        merchant.setRuc("12345678901");
        fillPerson(merchant);
        return merchant;
    }

    private SystemAdministrator adminProfile() {
        SystemAdministrator admin = new SystemAdministrator();
        admin.setUserAccount(account(13, "admin@kingstore.pe", "secret"));
        admin.setPosition("Admin");
        fillPerson(admin);
        return admin;
    }

    private void fillPerson(pe.edu.pucp.kingstore.domain.model.user.Person person) {
        person.setFirstName("Ana");
        person.setPaternalSurname("Perez");
        person.setMaternalSurname("Rojas");
        person.setDocumentType(DocumentType.DNI);
        person.setDocumentNumber("12345678");
        person.setBirthDate(LocalDate.of(1990, 1, 1));
        person.setGender(Gender.FEMALE);
        person.setPhone("999999999");
    }

    private CreateUserDTO createUserDTO(Role role) {
        CreateUserDTO dto = new CreateUserDTO();
        dto.setEmail("person@kingstore.pe");
        dto.setPassword("secret");
        dto.setFirstName("Ana");
        dto.setPaternalSurname("Perez");
        dto.setMaternalSurname("Rojas");
        dto.setDocumentType(DocumentType.DNI);
        dto.setDocumentNumber("12345678");
        dto.setBirthDate(LocalDate.of(1990, 1, 1));
        dto.setGender(Gender.FEMALE);
        dto.setPhone("999999999");
        dto.setRole(role);
        return dto;
    }

    private RegisterCustomerDTO registerCustomerDTO() {
        RegisterCustomerDTO dto = new RegisterCustomerDTO();
        dto.setEmail("new-customer@kingstore.pe");
        dto.setPassword("Password123!");
        dto.setFirstName("Ana");
        dto.setPaternalSurname("Perez");
        dto.setMaternalSurname("Rojas");
        dto.setDocumentType(DocumentType.DNI);
        dto.setDocumentNumber("12345678");
        dto.setBirthDate(LocalDate.of(1990, 1, 1));
        dto.setGender(Gender.FEMALE);
        dto.setPhone("999999999");
        return dto;
    }

    private LoginRequestDTO login(String email, String password) {
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setEmail(email);
        dto.setPassword(password);
        return dto;
    }
}