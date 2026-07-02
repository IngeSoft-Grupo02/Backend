package pe.edu.pucp.kingstore.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.audit.enums.AuditLevel;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.enums.Role;
import pe.edu.pucp.kingstore.repository.audit.AuditLogRepository;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;
import pe.edu.pucp.kingstore.service.security.JwtUtil;
import pe.edu.pucp.kingstore.service.storage.StorageService;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminBulkUploadSecurityIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JwtUtil jwtUtil;
    @Autowired StoreRepository storeRepository;
    @Autowired UserAccountRepository userAccountRepository;
    @Autowired AuditLogRepository auditLogRepository;

    @MockitoBean StorageService storageService;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
    }

    @Test
    void systemAdminMultipartUploadRejectsInvalidColorsBeforePersisting() throws Exception {
        long usersBefore = userAccountRepository.count();
        long storesBefore = storeRepository.count();

        mockMvc.perform(multipart("/admin/bulk/upload")
                        .file(csv("merchants", "merchants.csv", """
                                email,password,firstName,paternalSurname,maternalSurname,documentType,documentNumber,birthDate,phone,gender,ruc
                                test_bulk_invalid1@example.com,Pass1234!,TestBulk,MerchantUno,Demo,DNI,12345678,1988-05-15,987654321,MALE,20100000001
                                test_bulk_invalid2@example.com,Pass5678!,TestBulk,MerchantDos,Demo,DNI,87654321,1992-11-20,912345678,FEMALE,20200000002
                                """))
                        .file(csv("stores", "stores.csv", """
                                storeName,categoryId,primaryColor,secondaryColor,tertiaryColor,description,merchantEmail,logoFileName
                                Mi Tienda Urbana,1,ONYX_BLACK,OLIVE_DRAB,RICH_CAMEL,Ropa urbana y accesorios,test_bulk_invalid1@example.com,MiTiendaUrbana.jpg
                                Luxe Moda,1,MIDNIGHT,SAGE,RAW_GOLD,Moda premium y accesorios,test_bulk_invalid2@example.com,LuxeModa.jpg
                                """))
                        .file(zip("logos", "logos.zip", Map.of(
                                "logos/MiTiendaUrbana.jpg", "first-image".getBytes(StandardCharsets.UTF_8),
                                "logos/LuxeModa.jpg", "second-image".getBytes(StandardCharsets.UTF_8)
                        )))
                        .header("Authorization", "Bearer " + systemAdminToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCount").value(2))
                .andExpect(jsonPath("$.incidences[*].code", hasItem("VAL_COLOR")));

        assertThat(userAccountRepository.count()).isEqualTo(usersBefore);
        assertThat(storeRepository.count()).isEqualTo(storesBefore);
        assertThat(auditLogRepository.findAll()).anySatisfy(log -> {
            assertThat(log.getEndpoint()).isEqualTo("/admin/bulk/upload");
            assertThat(log.getStatusCode()).isEqualTo(400);
            assertThat(log.getLevel()).isEqualTo(AuditLevel.WARN);
        });
    }

    @Test
    void systemAdminMultipartUploadCreatesValidBatchWithNestedLogos() throws Exception {
        when(storageService.uploadBytes(anyString(), any(), anyString()))
                .thenAnswer(invocation -> "http://localhost/uploads/" + invocation.getArgument(0, String.class));

        mockMvc.perform(multipart("/admin/bulk/upload")
                        .file(csv("merchants", "merchants.csv", """
                                email,password,firstName,paternalSurname,maternalSurname,documentType,documentNumber,birthDate,phone,gender,ruc
                                test_bulk_valid1@example.com,Pass1234!,TestBulk,MerchantUno,Demo,DNI,22345678,1988-05-15,987654321,MALE,20100000011
                                test_bulk_valid2@example.com,Pass5678!,TestBulk,MerchantDos,Demo,DNI,27654321,1992-11-20,912345678,FEMALE,20200000012
                                """))
                        .file(csv("stores", "stores.csv", """
                                storeName,categoryId,primaryColor,secondaryColor,tertiaryColor,description,merchantEmail,logoFileName
                                TEST_BULK_Mi Tienda Urbana,1,ONYX_BLACK,SLATE,RAW_GOLD,Ropa urbana y accesorios,test_bulk_valid1@example.com,MiTiendaUrbana.jpg
                                TEST_BULK_Luxe Moda,1,MIDNIGHT,SAGE,COPPER,Moda premium y accesorios,test_bulk_valid2@example.com,LuxeModa.jpg
                                """))
                        .file(zip("logos", "logos.zip", Map.of(
                                "logos/MiTiendaUrbana.jpg", "first-image".getBytes(StandardCharsets.UTF_8),
                                "logos/LuxeModa.jpg", "second-image".getBytes(StandardCharsets.UTF_8)
                        )))
                        .header("Authorization", "Bearer " + systemAdminToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorCount").value(0))
                .andExpect(jsonPath("$.merchantsCreated").value(2))
                .andExpect(jsonPath("$.storesCreated").value(2))
                .andExpect(jsonPath("$.logosUploaded").value(2));

        Optional<Store> firstStore = storeRepository.findBySlug("test-bulk-mi-tienda-urbana");
        Optional<Store> secondStore = storeRepository.findBySlug("test-bulk-luxe-moda");
        assertThat(firstStore).isPresent();
        assertThat(secondStore).isPresent();
        assertThat(firstStore.get().getLogoUrl()).isNotBlank();
        assertThat(secondStore.get().getLogoUrl()).isNotBlank();
        assertThat(auditLogRepository.findAll()).anySatisfy(log -> {
            assertThat(log.getEndpoint()).isEqualTo("/admin/bulk/upload");
            assertThat(log.getStatusCode()).isEqualTo(200);
            assertThat(log.getLevel()).isEqualTo(AuditLevel.INFO);
        });
    }

    @Test
    void bulkUploadEndpointRequiresSystemAdminRole() throws Exception {
        MockMultipartFile merchants = csv("merchants", "merchants.csv", "email\nx@test.com\n");

        mockMvc.perform(multipart("/admin/bulk/upload").file(merchants))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/admin/bulk/upload")
                        .file(merchants)
                        .header("Authorization", "Bearer "
                                + jwtUtil.generateToken(200, "merchant@test.com", Role.MERCHANT, null)))
                .andExpect(status().isForbidden());
    }

    private String systemAdminToken() {
        return jwtUtil.generateToken(100, "test_bulk_admin@example.com", Role.SYSTEM_ADMIN, null);
    }

    private MockMultipartFile csv(String partName, String filename, String content) {
        return new MockMultipartFile(partName, filename, "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile zip(String partName, String filename, Map<String, byte[]> entries) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output)) {
            for (Map.Entry<String, byte[]> entry : entries.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue());
                zip.closeEntry();
            }
        }
        return new MockMultipartFile(partName, filename, "application/zip", output.toByteArray());
    }
}
