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
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
                King Store,king-store,1,ONYX_BLACK,OLIVE_DRAB,RICH_CAMEL,Main store,merchant@kingstore.pe,logo.png
                """);
        when(storeRepository.findBySlug("king-store")).thenReturn(Optional.empty());
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
}
