package pe.edu.pucp.kingstore.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import pe.edu.pucp.kingstore.domain.dto.bulk.BulkIncidenceDTO;
import pe.edu.pucp.kingstore.domain.dto.bulk.BulkUploadResponseDTO;
import pe.edu.pucp.kingstore.domain.dto.store.StoreDTO;
import pe.edu.pucp.kingstore.domain.dto.user.CreateUserDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.StoreCategory;
import pe.edu.pucp.kingstore.domain.model.store.enums.PrimaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.SecondaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.domain.model.store.enums.TertiaryColor;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
import pe.edu.pucp.kingstore.domain.model.user.enums.DocumentType;
import pe.edu.pucp.kingstore.domain.model.user.enums.Gender;
import pe.edu.pucp.kingstore.repository.store.StoreCategoryRepository;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;
import pe.edu.pucp.kingstore.service.bulk.BulkUploadService;
import pe.edu.pucp.kingstore.service.storage.StorageService;
import pe.edu.pucp.kingstore.service.store.StoreService;
import pe.edu.pucp.kingstore.service.user.UserAccountService;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BulkUploadServiceCoverageTest {

    @Mock UserAccountService userAccountService;
    @Mock StoreService storeService;
    @Mock StoreRepository storeRepository;
    @Mock UserAccountRepository userAccountRepository;
    @Mock MerchantRepository merchantRepository;
    @Mock StoreCategoryRepository categoryRepository;
    @Mock StorageService storageService;

    private BulkUploadService service;

    @BeforeEach
    void setUp() {
        service = new BulkUploadService(
                userAccountService,
                storeService,
                storeRepository,
                userAccountRepository,
                merchantRepository,
                categoryRepository,
                storageService
        );
    }

    @Test
    void processCreatesValidMerchantsFromCsv() throws Exception {
        MockMultipartFile merchants = csv("merchants.csv", """
                email,password,firstName,paternalSurname,maternalSurname,documentType,documentNumber,birthDate,phone,gender,ruc
                Merchant@Kingstore.pe,secret,Ana,Perez,Rojas,DNI,12345678,1990-01-20,999999999,FEMALE,12345678901
                """);
        when(userAccountRepository.findByEmail("merchant@kingstore.pe")).thenReturn(Optional.empty());

        BulkUploadResponseDTO response = service.process(merchants, null, null);

        assertThat(response.getMerchantsProcessed()).isEqualTo(1);
        assertThat(response.getMerchantsCreated()).isEqualTo(1);
        assertThat(response.getErrorCount()).isZero();
        ArgumentCaptor<CreateUserDTO> captor = ArgumentCaptor.forClass(CreateUserDTO.class);
        verify(userAccountService).createWithRole(captor.capture());
        assertThat(captor.getValue().getRuc()).isEqualTo("12345678901");
    }

    @Test
    void processCollectsMerchantValidationErrorsAndSkipsCreation() throws Exception {
        MockMultipartFile merchants = csv("merchants.csv", """
                email,password,firstName,paternalSurname,maternalSurname,documentType,documentNumber,birthDate,phone,gender,ruc
                bad-email,,,"Perez","Rojas",BAD,12345678,not-a-date,999,OTHER,123
                """);

        BulkUploadResponseDTO response = service.process(merchants, null, null);

        assertThat(response.getMerchantsProcessed()).isEqualTo(1);
        assertThat(response.getMerchantsCreated()).isZero();
        assertThat(response.getErrorCount()).isGreaterThanOrEqualTo(6);
        assertThat(response.getIncidences())
                .extracting(BulkIncidenceDTO::getBlock)
                .containsOnly(BulkIncidenceDTO.IncidenceBlock.MERCHANTS);
        verify(userAccountService, never()).createWithRole(any());
    }

    @Test
    void existingMerchantWithSameDataIsSkipped() throws Exception {
        UserAccount account = account(8, "merchant@kingstore.pe", "secret");
        Merchant merchant = matchingMerchant(9, account);
        MockMultipartFile merchants = csv("merchants.csv", """
                email,password,firstName,paternalSurname,maternalSurname,documentType,documentNumber,birthDate,phone,gender,ruc
                merchant@kingstore.pe,secret,Ana,Perez,Rojas,DNI,12345678,1990-01-20,999999999,FEMALE,12345678901
                """);
        when(userAccountRepository.findByEmail("merchant@kingstore.pe")).thenReturn(Optional.of(account));
        when(merchantRepository.findByUserAccountId(8)).thenReturn(Optional.of(merchant));

        BulkUploadResponseDTO response = service.process(merchants, null, null);

        assertThat(response.getErrorCount()).isZero();
        assertThat(response.getMerchantsCreated()).isZero();
        assertThat(response.getIncidences()).extracting(BulkIncidenceDTO::getCode)
                .contains("SKIPPED_EXISTING");
        verify(userAccountService, never()).createWithRole(any());
    }

    @Test
    void existingMerchantWithDifferentDataIsConflict() throws Exception {
        UserAccount account = account(8, "merchant@kingstore.pe", "secret");
        Merchant merchant = matchingMerchant(9, account);
        MockMultipartFile merchants = csv("merchants.csv", """
                email,password,firstName,paternalSurname,maternalSurname,documentType,documentNumber,birthDate,phone,gender,ruc
                merchant@kingstore.pe,secret,AnaDistinta,Perez,Rojas,DNI,12345678,1990-01-20,999999999,FEMALE,12345678901
                """);
        when(userAccountRepository.findByEmail("merchant@kingstore.pe")).thenReturn(Optional.of(account));
        when(merchantRepository.findByUserAccountId(8)).thenReturn(Optional.of(merchant));

        BulkUploadResponseDTO response = service.process(merchants, null, null);

        assertThat(response.getErrorCount()).isEqualTo(1);
        assertThat(response.getIncidences()).extracting(BulkIncidenceDTO::getCode).contains("CONFLICT");
        verify(userAccountService, never()).createWithRole(any());
    }

    @Test
    void storesOnlyWithMissingMerchantReturnsValidationError() throws Exception {
        StoreCategory category = category(1);
        MockMultipartFile stores = csv("stores.csv", """
                storeName,categoryId,primaryColor,secondaryColor,tertiaryColor,description,merchantEmail,logoFileName
                King Store,1,ONYX_BLACK,SLATE,RAW_GOLD,Main store,missing@kingstore.pe,
                """);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(userAccountRepository.findByEmail("missing@kingstore.pe")).thenReturn(Optional.empty());

        BulkUploadResponseDTO response = service.process(null, stores, null);

        assertThat(response.getStoresCreated()).isZero();
        assertThat(response.getIncidences()).extracting(BulkIncidenceDTO::getCode).contains("REF_NOT_FOUND");
        verify(storeService, never()).createFromDTO(any());
    }

    @Test
    void merchantsAndStoresAllowsNewValidMerchantFromSameBatch() throws Exception {
        StoreCategory category = category(1);
        UserAccount account = account(8, "new@kingstore.pe", "secret");
        Merchant merchant = matchingMerchant(9, account);
        MockMultipartFile merchants = csv("merchants.csv", """
                email,password,firstName,paternalSurname,maternalSurname,documentType,documentNumber,birthDate,phone,gender,ruc
                new@kingstore.pe,secret,Ana,Perez,Rojas,DNI,12345678,1990-01-20,999999999,FEMALE,12345678901
                """);
        MockMultipartFile stores = csv("stores.csv", """
                storeName,categoryId,primaryColor,secondaryColor,tertiaryColor,description,merchantEmail,logoFileName
                King Store,1,ONYX_BLACK,SLATE,RAW_GOLD,Main store,new@kingstore.pe,
                """);
        when(userAccountRepository.findByEmail("new@kingstore.pe"))
                .thenReturn(Optional.empty(), Optional.of(account));
        when(merchantRepository.findByUserAccountId(8)).thenReturn(Optional.of(merchant));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));

        BulkUploadResponseDTO response = service.process(merchants, stores, null);

        assertThat(response.getErrorCount()).isZero();
        assertThat(response.getMerchantsCreated()).isEqualTo(1);
        assertThat(response.getStoresCreated()).isEqualTo(1);
        verify(userAccountService).createWithRole(any());
        verify(storeService).createFromDTO(any());
    }

    @Test
    void merchantsAndStoresRejectsStoreWhenBatchMerchantIsInvalid() throws Exception {
        StoreCategory category = category(1);
        MockMultipartFile merchants = csv("merchants.csv", """
                email,password,firstName,paternalSurname,maternalSurname,documentType,documentNumber,birthDate,phone,gender,ruc
                bad@kingstore.pe,secret,Ana,Perez,Rojas,DNI,12345678,1990-01-20,999999999,FEMALE,123
                """);
        MockMultipartFile stores = csv("stores.csv", """
                storeName,categoryId,primaryColor,secondaryColor,tertiaryColor,description,merchantEmail,logoFileName
                King Store,1,ONYX_BLACK,SLATE,RAW_GOLD,Main store,bad@kingstore.pe,
                """);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));

        BulkUploadResponseDTO response = service.process(merchants, stores, null);

        assertThat(response.getErrorCount()).isGreaterThanOrEqualTo(2);
        assertThat(response.getIncidences()).extracting(BulkIncidenceDTO::getCode)
                .contains("VAL_RUC", "REF_INVALID");
        verify(userAccountService, never()).createWithRole(any());
        verify(storeService, never()).createFromDTO(any());
    }

    @Test
    void processCreatesStoresFromCsvWithCategoryColorsAndMerchantWithoutLogo() throws Exception {
        StoreCategory category = category(1);
        UserAccount account = account(8, "merchant@kingstore.pe", "secret");
        Merchant merchant = matchingMerchant(9, account);
        MockMultipartFile stores = csv("stores.csv", """
                storeName,categoryId,primaryColor,secondaryColor,tertiaryColor,description,merchantEmail,logoFileName
                King Store,1,ONYX_BLACK,SLATE,RAW_GOLD,Main store,merchant@kingstore.pe,
                """);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(userAccountRepository.findByEmail("merchant@kingstore.pe")).thenReturn(Optional.of(account));
        when(merchantRepository.findByUserAccountId(8)).thenReturn(Optional.of(merchant));

        BulkUploadResponseDTO response = service.process(null, stores, null);

        assertThat(response.getStoresProcessed()).isEqualTo(1);
        assertThat(response.getStoresCreated()).isEqualTo(1);
        assertThat(response.getErrorCount()).isZero();
        ArgumentCaptor<StoreDTO> captor = ArgumentCaptor.forClass(StoreDTO.class);
        verify(storeService).createFromDTO(captor.capture());
        assertThat(captor.getValue().getCategoryId()).isEqualTo(1);
        assertThat(captor.getValue().getPrimaryColor()).isEqualTo(PrimaryColor.ONYX_BLACK);
        assertThat(captor.getValue().getMerchantId()).isEqualTo(9);
        assertThat(captor.getValue().getSlug()).isNull();
    }

    @Test
    void existingStoreWithSameDataIsSkipped() throws Exception {
        StoreCategory category = category(1);
        UserAccount account = account(8, "merchant@kingstore.pe", "secret");
        Merchant merchant = matchingMerchant(9, account);
        Store store = store("King Store", "king-store", "Main store", category, merchant);
        MockMultipartFile stores = csv("stores.csv", """
                storeName,categoryId,primaryColor,secondaryColor,tertiaryColor,description,merchantEmail,logoFileName
                King Store,1,ONYX_BLACK,SLATE,RAW_GOLD,Main store,merchant@kingstore.pe,
                """);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(userAccountRepository.findByEmail("merchant@kingstore.pe")).thenReturn(Optional.of(account));
        when(merchantRepository.findByUserAccountId(8)).thenReturn(Optional.of(merchant));
        when(storeRepository.findBySlug("king-store")).thenReturn(Optional.of(store));

        BulkUploadResponseDTO response = service.process(null, stores, null);

        assertThat(response.getErrorCount()).isZero();
        assertThat(response.getStoresCreated()).isZero();
        assertThat(response.getIncidences()).extracting(BulkIncidenceDTO::getCode)
                .contains("SKIPPED_EXISTING");
        verify(storeService, never()).createFromDTO(any());
    }

    @Test
    void existingStoreWithDifferentDataIsConflict() throws Exception {
        StoreCategory category = category(1);
        UserAccount account = account(8, "merchant@kingstore.pe", "secret");
        Merchant merchant = matchingMerchant(9, account);
        Store store = store("King Store", "king-store", "Different description", category, merchant);
        MockMultipartFile stores = csv("stores.csv", """
                storeName,categoryId,primaryColor,secondaryColor,tertiaryColor,description,merchantEmail,logoFileName
                King Store,1,ONYX_BLACK,SLATE,RAW_GOLD,Main store,merchant@kingstore.pe,
                """);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(userAccountRepository.findByEmail("merchant@kingstore.pe")).thenReturn(Optional.of(account));
        when(merchantRepository.findByUserAccountId(8)).thenReturn(Optional.of(merchant));
        when(storeRepository.findBySlug("king-store")).thenReturn(Optional.of(store));

        BulkUploadResponseDTO response = service.process(null, stores, null);

        assertThat(response.getErrorCount()).isEqualTo(1);
        assertThat(response.getStoresCreated()).isZero();
        assertThat(response.getIncidences()).extracting(BulkIncidenceDTO::getCode).contains("CONFLICT");
        verify(storeService, never()).createFromDTO(any());
    }

    @Test
    void storeLogoFileNameWithoutZipIsValidationError() throws Exception {
        StoreCategory category = category(1);
        UserAccount account = account(8, "merchant@kingstore.pe", "secret");
        Merchant merchant = matchingMerchant(9, account);
        MockMultipartFile stores = csv("stores.csv", """
                storeName,categoryId,primaryColor,secondaryColor,tertiaryColor,description,merchantEmail,logoFileName
                King Store,1,ONYX_BLACK,SLATE,RAW_GOLD,Main store,merchant@kingstore.pe,logo.png
                """);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(userAccountRepository.findByEmail("merchant@kingstore.pe")).thenReturn(Optional.of(account));
        when(merchantRepository.findByUserAccountId(8)).thenReturn(Optional.of(merchant));

        BulkUploadResponseDTO response = service.process(null, stores, null);

        assertThat(response.getErrorCount()).isEqualTo(1);
        assertThat(response.getIncidences()).extracting(BulkIncidenceDTO::getCode).contains("VAL_LOGO");
        verify(storeService, never()).createFromDTO(any());
    }

    @Test
    void storeLogoFileNameMissingFromZipIsValidationError() throws Exception {
        StoreCategory category = category(1);
        UserAccount account = account(8, "merchant@kingstore.pe", "secret");
        Merchant merchant = matchingMerchant(9, account);
        MockMultipartFile stores = csv("stores.csv", """
                storeName,categoryId,primaryColor,secondaryColor,tertiaryColor,description,merchantEmail,logoFileName
                King Store,1,ONYX_BLACK,SLATE,RAW_GOLD,Main store,merchant@kingstore.pe,logo.png
                """);
        MockMultipartFile logos = zip("logos.zip", "other.png", "image".getBytes(StandardCharsets.UTF_8));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(userAccountRepository.findByEmail("merchant@kingstore.pe")).thenReturn(Optional.of(account));
        when(merchantRepository.findByUserAccountId(8)).thenReturn(Optional.of(merchant));

        BulkUploadResponseDTO response = service.process(null, stores, logos);

        assertThat(response.getErrorCount()).isEqualTo(1);
        assertThat(response.getIncidences()).extracting(BulkIncidenceDTO::getCode).contains("REF_NOT_FOUND");
        verify(storeService, never()).createFromDTO(any());
        verify(storageService, never()).uploadBytes(any(), any(), any());
    }

    @Test
    void processReportsUnsupportedDbColorsBeforePersistingStores() throws Exception {
        StoreCategory category = category(1);
        UserAccount account = account(8, "merchant@kingstore.pe", "secret");
        Merchant merchant = matchingMerchant(9, account);
        MockMultipartFile stores = csv("stores.csv", """
                storeName,categoryId,primaryColor,secondaryColor,tertiaryColor,description,merchantEmail,logoFileName
                Mi Tienda Urbana,1,ONYX_BLACK,OLIVE_DRAB,RICH_CAMEL,Main store,merchant@kingstore.pe,MiTiendaUrbana.jpg
                """);
        MockMultipartFile logos = zip("logos.zip", "logos/MiTiendaUrbana.jpg", "image-bytes".getBytes(StandardCharsets.UTF_8));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(userAccountRepository.findByEmail("merchant@kingstore.pe")).thenReturn(Optional.of(account));
        when(merchantRepository.findByUserAccountId(8)).thenReturn(Optional.of(merchant));

        BulkUploadResponseDTO response = service.process(null, stores, logos);

        assertThat(response.getStoresProcessed()).isEqualTo(1);
        assertThat(response.getStoresCreated()).isZero();
        assertThat(response.getLogosUploaded()).isZero();
        assertThat(response.getIncidences()).extracting(BulkIncidenceDTO::getCode)
                .containsOnly("VAL_COLOR");
        assertThat(response.getIncidences()).extracting(BulkIncidenceDTO::getDetail)
                .anySatisfy(detail -> assertThat(detail).contains("secondaryColor", "OLIVE_DRAB"))
                .anySatisfy(detail -> assertThat(detail).contains("tertiaryColor", "RICH_CAMEL"));
        verify(storeService, never()).createFromDTO(any());
        verify(storageService, never()).uploadBytes(any(), any(), any());
    }

    @Test
    void processUploadsReferencedLogoAndUpdatesExistingStoreWhenOnlyZipIsProvided() throws Exception {
        Store store = new Store();
        store.setId(7);
        store.setSlug("king-store");
        MockMultipartFile logos = zip("logos.zip", "folder/king-store.png", "image-bytes".getBytes(StandardCharsets.UTF_8));
        when(storeRepository.findBySlug("king-store")).thenReturn(Optional.of(store));
        when(storageService.uploadBytes(eq("logos/king-store.png"), any(), eq("image/png")))
                .thenReturn("https://cdn.test/logos/king-store.png");

        BulkUploadResponseDTO response = service.process(null, null, logos);

        assertThat(response.getLogosUploaded()).isEqualTo(1);
        assertThat(store.getLogoUrl()).isEqualTo("https://cdn.test/logos/king-store.png");
        verify(storeRepository).save(store);
    }

    @Test
    void processCreatesStoresAndUploadsNestedLogosByLogoFileNameBasename() throws Exception {
        StoreCategory category = category(1);
        UserAccount firstAccount = account(8, "merchant1@kingstore.pe", "secret");
        UserAccount secondAccount = account(10, "merchant2@kingstore.pe", "secret");
        Merchant firstMerchant = matchingMerchant(9, firstAccount);
        Merchant secondMerchant = matchingMerchant(11, secondAccount);
        MockMultipartFile stores = csv("stores.csv", """
                storeName,categoryId,primaryColor,secondaryColor,tertiaryColor,description,merchantEmail,logoFileName
                Mi Tienda Urbana,1,ONYX_BLACK,SLATE,RAW_GOLD,Main store,merchant1@kingstore.pe,MiTiendaUrbana.jpg
                Luxe Moda,1,MIDNIGHT,SAGE,COPPER,Second store,merchant2@kingstore.pe,LuxeModa.jpg
                """);
        MockMultipartFile logos = zip("logos.zip", Map.of(
                "logos/MiTiendaUrbana.jpg", "first-image".getBytes(StandardCharsets.UTF_8),
                "logos/LuxeModa.jpg", "second-image".getBytes(StandardCharsets.UTF_8)
        ));
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(userAccountRepository.findByEmail("merchant1@kingstore.pe")).thenReturn(Optional.of(firstAccount));
        when(userAccountRepository.findByEmail("merchant2@kingstore.pe")).thenReturn(Optional.of(secondAccount));
        when(merchantRepository.findByUserAccountId(8)).thenReturn(Optional.of(firstMerchant));
        when(merchantRepository.findByUserAccountId(10)).thenReturn(Optional.of(secondMerchant));
        when(storeService.createFromDTO(any())).thenAnswer(invocation -> {
            StoreDTO dto = invocation.getArgument(0);
            Store store = new Store();
            store.setId(dto.getStoreName().startsWith("Mi") ? 1 : 2);
            store.setSlug(dto.getStoreName().startsWith("Mi") ? "mi-tienda-urbana" : "luxe-moda");
            return store;
        });
        when(storageService.uploadBytes(eq("logos/mi-tienda-urbana.jpg"), any(), eq("image/jpeg")))
                .thenReturn("https://cdn.test/logos/mi-tienda-urbana.jpg");
        when(storageService.uploadBytes(eq("logos/luxe-moda.jpg"), any(), eq("image/jpeg")))
                .thenReturn("https://cdn.test/logos/luxe-moda.jpg");

        BulkUploadResponseDTO response = service.process(null, stores, logos);

        assertThat(response.getStoresCreated()).isEqualTo(2);
        assertThat(response.getLogosUploaded()).isEqualTo(2);
        assertThat(response.getErrorCount()).isZero();
        verify(storeRepository, times(2)).save(any(Store.class));
    }

    @Test
    void unexpectedPersistenceFailuresStillPropagateForRollback() throws Exception {
        when(userAccountRepository.findByEmail("unexpected@kingstore.pe")).thenReturn(Optional.empty());
        doThrow(new RuntimeException("create failed")).when(userAccountService).createWithRole(any());
        MockMultipartFile unexpectedMerchants = csv("merchants.csv", """
                email,password,firstName,paternalSurname,maternalSurname,documentType,documentNumber,birthDate,phone,gender,ruc
                unexpected@kingstore.pe,secret,Ana,Perez,Rojas,DNI,12345678,1990-01-20,999999999,FEMALE,12345678901
                """);

        assertThatThrownBy(() -> service.process(unexpectedMerchants, null, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("create failed");
    }

    private MockMultipartFile csv(String name, String content) {
        return new MockMultipartFile(name, name, "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile zip(String name, String entryName, byte[] content) throws Exception {
        return zip(name, Map.of(entryName, content));
    }

    private MockMultipartFile zip(String name, Map<String, byte[]> entries) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return new MockMultipartFile(name, name, "application/zip", output.toByteArray());
    }

    private StoreCategory category(Integer id) {
        StoreCategory category = new StoreCategory();
        category.setId(id);
        category.setStoreCategoryName("Moda");
        return category;
    }

    private UserAccount account(Integer id, String email, String password) {
        UserAccount account = new UserAccount();
        account.setId(id);
        account.setEmail(email);
        account.setPassword(password);
        return account;
    }

    private Merchant matchingMerchant(Integer id, UserAccount account) {
        Merchant merchant = new Merchant();
        merchant.setId(id);
        merchant.setUserAccount(account);
        merchant.setFirstName("Ana");
        merchant.setPaternalSurname("Perez");
        merchant.setMaternalSurname("Rojas");
        merchant.setDocumentType(DocumentType.DNI);
        merchant.setDocumentNumber("12345678");
        merchant.setBirthDate(LocalDate.parse("1990-01-20"));
        merchant.setPhone("999999999");
        merchant.setGender(Gender.FEMALE);
        merchant.setRuc("12345678901");
        return merchant;
    }

    private Store store(String name, String slug, String description, StoreCategory category, Merchant merchant) {
        Store store = new Store();
        store.setId(20);
        store.setStoreName(name);
        store.setSlug(slug);
        store.setDescription(description);
        store.setCategory(category);
        store.setMerchant(merchant);
        store.setPrimaryColor(PrimaryColor.ONYX_BLACK);
        store.setSecondaryColor(SecondaryColor.SLATE);
        store.setTertiaryColor(TertiaryColor.RAW_GOLD);
        store.setStoreStatus(StoreStatus.ACTIVE);
        return store;
    }
}
