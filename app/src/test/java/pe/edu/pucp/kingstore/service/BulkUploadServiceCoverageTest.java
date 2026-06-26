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
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
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
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
    void processCreatesStoresFromCsvWithCategoryColorsAndMerchant() throws Exception {
        StoreCategory category = new StoreCategory();
        category.setId(1);
        UserAccount account = new UserAccount();
        account.setId(8);
        account.setEmail("merchant@kingstore.pe");
        Merchant merchant = new Merchant();
        merchant.setId(9);
        MockMultipartFile stores = csv("stores.csv", """
                storeName,slug,categoryId,primaryColor,secondaryColor,tertiaryColor,description,merchantEmail,logoFileName
                King Store,king-store,1,ONYX_BLACK,SLATE,RAW_GOLD,Main store,merchant@kingstore.pe,logo.png
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
    void bulkUpload_generatesSlugAutomatically() throws Exception {
        StoreCategory category = new StoreCategory();
        category.setId(1);
        UserAccount account = new UserAccount();
        account.setId(8);
        Merchant merchant = new Merchant();
        merchant.setId(9);
        MockMultipartFile stores = csv("stores.csv", """
                storeName,categoryId,primaryColor,secondaryColor,tertiaryColor,description,merchantEmail,logoFileName
                Hilos Urbanos,1,ONYX_BLACK,SLATE,RAW_GOLD,Main store,merchant@kingstore.pe,logo.png
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
        assertThat(captor.getValue().getStoreName()).isEqualTo("Hilos Urbanos");
        assertThat(captor.getValue().getSlug()).isNull();
    }

    @Test
    void processReportsStoreValidationErrorsAndLogoWarnings() throws Exception {
        MockMultipartFile stores = csv("stores.csv", """
                storeName,slug,categoryId,primaryColor,secondaryColor,tertiaryColor,description,merchantEmail,logoFileName
                ,king-store,abc,BAD,BAD,BAD,Main store,,logo.png
                """);
        MockMultipartFile logos = zip("logos.zip", "readme.txt", "not-image".getBytes(StandardCharsets.UTF_8));

        BulkUploadResponseDTO response = service.process(null, stores, logos);

        assertThat(response.getStoresCreated()).isZero();
        assertThat(response.getLogosUploaded()).isZero();
        assertThat(response.getErrorCount()).isGreaterThanOrEqualTo(6);
        assertThat(response.getIncidences())
                .extracting(BulkIncidenceDTO::getBlock)
                .contains(BulkIncidenceDTO.IncidenceBlock.STORES, BulkIncidenceDTO.IncidenceBlock.IMAGES);
        verify(storeService, never()).createFromDTO(any());
    }

    @Test
    void processUploadsReferencedLogoAndUpdatesStore() throws Exception {
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
    void processReportsDuplicateReferencesUnexpectedFailuresAndLogoEdgeWarnings() throws Exception {
        UserAccount duplicate = new UserAccount();
        duplicate.setId(1);
        duplicate.setEmail("duplicate@kingstore.pe");
        when(userAccountRepository.findByEmail("duplicate@kingstore.pe")).thenReturn(Optional.of(duplicate));
        MockMultipartFile duplicateMerchants = csv("merchants.csv", """
                email,password,firstName,paternalSurname,maternalSurname,documentType,documentNumber,birthDate,phone,gender,ruc
                duplicate@kingstore.pe,secret,Ana,Perez,Rojas,DNI,12345678,1990-01-20,999999999,FEMALE,12345678901
                """);

        BulkUploadResponseDTO duplicateResponse = service.process(duplicateMerchants, null, null);

        assertThat(duplicateResponse.getMerchantsCreated()).isZero();
        assertThat(duplicateResponse.getIncidences()).extracting(BulkIncidenceDTO::getCode).contains("DUPLICATE");

        when(userAccountRepository.findByEmail("unexpected@kingstore.pe")).thenReturn(Optional.empty());
        doThrow(new RuntimeException("create failed")).when(userAccountService).createWithRole(any());
        MockMultipartFile unexpectedMerchants = csv("merchants.csv", """
                email,password,firstName,paternalSurname,maternalSurname,documentType,documentNumber,birthDate,phone,gender,ruc

                unexpected@kingstore.pe,secret,Ana,Perez,Rojas,DNI,12345678,1990-01-20,999999999,FEMALE,12345678901
                """);

        BulkUploadResponseDTO unexpectedMerchantResponse = service.process(unexpectedMerchants, null, null);

        assertThat(unexpectedMerchantResponse.getMerchantsCreated()).isZero();
        assertThat(unexpectedMerchantResponse.getIncidences()).extracting(BulkIncidenceDTO::getCode).contains("UNEXPECTED");

        Store existingStore = new Store();
        existingStore.setId(2);
        when(categoryRepository.findById(99)).thenReturn(Optional.empty());
        String longName = "A".repeat(101);
        MockMultipartFile invalidStores = csv("stores.csv", """
                storeName,slug,categoryId,primaryColor,secondaryColor,tertiaryColor,description,merchantEmail,logoFileName
                %s,taken-store,99,ONYX_BLACK,SLATE,RAW_GOLD,Desc,,logo.png
                """.formatted(longName));

        BulkUploadResponseDTO invalidStoreResponse = service.process(null, invalidStores, null);

        assertThat(invalidStoreResponse.getStoresCreated()).isZero();
        assertThat(invalidStoreResponse.getIncidences()).extracting(BulkIncidenceDTO::getCode)
                .contains("VAL_NAME", "REF_NOT_FOUND", "VAL_MERCHANT");

        StoreCategory category = new StoreCategory();
        category.setId(1);
        UserAccount account = new UserAccount();
        account.setId(8);
        Merchant merchant = new Merchant();
        merchant.setId(9);
        when(categoryRepository.findById(1)).thenReturn(Optional.of(category));
        when(userAccountRepository.findByEmail("owner@kingstore.pe")).thenReturn(Optional.of(account));
        when(merchantRepository.findByUserAccountId(8)).thenReturn(Optional.of(merchant));
        doThrow(new RuntimeException("store failed")).when(storeService).createFromDTO(any());
        MockMultipartFile unexpectedStores = csv("stores.csv", """
                storeName,slug,categoryId,primaryColor,secondaryColor,tertiaryColor,description,merchantEmail,logoFileName
                Boom Store,boom-store,1,ONYX_BLACK,SLATE,RAW_GOLD,Desc,owner@kingstore.pe,logo.png
                """);

        BulkUploadResponseDTO unexpectedStoreResponse = service.process(null, unexpectedStores, null);

        assertThat(unexpectedStoreResponse.getStoresCreated()).isZero();
        assertThat(unexpectedStoreResponse.getIncidences()).extracting(BulkIncidenceDTO::getCode).contains("UNEXPECTED");

        when(storeRepository.findBySlug("missing-store")).thenReturn(Optional.empty());
        BulkUploadResponseDTO logoResponse = service.process(null, null, logoEdgeZip());

        assertThat(logoResponse.getLogosUploaded()).isZero();
        assertThat(logoResponse.getIncidences()).extracting(BulkIncidenceDTO::getCode)
                .contains("SIZE_EXCEEDED", "REF_NOT_FOUND");
    }

    private MockMultipartFile csv(String name, String content) {
        return new MockMultipartFile(name, name, "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile zip(String name, String entryName, byte[] content) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry(entryName));
            zip.write(content);
            zip.closeEntry();
        }
        return new MockMultipartFile(name, name, "application/zip", output.toByteArray());
    }

    private MockMultipartFile logoEdgeZip() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            zip.putNextEntry(new ZipEntry("folder/"));
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("huge.png"));
            zip.write(new byte[(2 * 1024 * 1024) + 1]);
            zip.closeEntry();

            zip.putNextEntry(new ZipEntry("missing-store.jpg"));
            zip.write("image".getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return new MockMultipartFile("logos", "logos.zip", "application/zip", output.toByteArray());
    }
}
