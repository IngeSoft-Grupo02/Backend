package pe.edu.pucp.kingstore.api.controller.admin;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.kingstore.domain.dto.bulk.BulkUploadResponseDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;
import pe.edu.pucp.kingstore.service.bulk.BulkUploadService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/bulk")
public class BulkUploadController {

    private final BulkUploadService     bulkUploadService;
    private final UserAccountRepository userAccountRepository;
    private final MerchantRepository    merchantRepository;
    private final StoreRepository       storeRepository;

    public BulkUploadController(
            BulkUploadService bulkUploadService,
            UserAccountRepository userAccountRepository,
            MerchantRepository merchantRepository,
            StoreRepository storeRepository) {
        this.bulkUploadService     = bulkUploadService;
        this.userAccountRepository = userAccountRepository;
        this.merchantRepository    = merchantRepository;
        this.storeRepository       = storeRepository;
    }

    // â”€â”€ Carga masiva â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> upload(
            @RequestPart(value = "merchants", required = false) MultipartFile merchantsCsv,
            @RequestPart(value = "stores",    required = false) MultipartFile storesCsv,
            @RequestPart(value = "logos",     required = false) MultipartFile logosZip) {
        try {
            if (isEmpty(merchantsCsv) && isEmpty(storesCsv) && isEmpty(logosZip))
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Debes enviar al menos un archivo (merchants, stores o logos)."));
            BulkUploadResponseDTO result = bulkUploadService.process(merchantsCsv, storesCsv, logosZip);
            if (result.getErrorCount() > 0) {
                return ResponseEntity.badRequest().body(result);
            }
            return ResponseEntity.ok(result);
        } catch (BusinessRuleException | IllegalArgumentException | DataIntegrityViolationException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "No se pudo completar la carga masiva: " + safeMessage(e)));
        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Error procesando archivos: " + safeMessage(e)));
        }
    }

    // â”€â”€ Datos existentes en BD para validaciÃ³n frontend â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/existing-emails")
    public ResponseEntity<List<String>> existingEmails() {
        List<String> emails = userAccountRepository.findAll().stream()
                .map(ua -> ua.getEmail().toLowerCase()).toList();
        return ResponseEntity.ok(emails);
    }

    @GetMapping("/existing-stores")
    public ResponseEntity<Map<String, Object>> existingStores() {
        List<Store> stores = storeRepository.findAll();
        List<String> storeNames = stores.stream()
                .filter(s -> s.getStoreName() != null)
                .map(s -> s.getStoreName().toLowerCase()).toList();
        List<Merchant> merchants = merchantRepository.findAll();
        List<String> merchantEmails = merchants.stream()
                .filter(m -> m.getUserAccount() != null && m.getUserAccount().getEmail() != null)
                .map(m -> m.getUserAccount().getEmail().toLowerCase()).toList();
        return ResponseEntity.ok(Map.of(
                "storeNames", storeNames,
                "merchantEmails", merchantEmails,
                "merchants", merchants.stream().map(this::merchantSnapshot).toList(),
                "stores", stores.stream().map(this::storeSnapshot).toList()
        ));
    }

    // â”€â”€ Plantillas â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/template/merchants")
    public ResponseEntity<byte[]> templateMerchants() {
        String csv = """
                email,password,firstName,paternalSurname,maternalSurname,documentType,documentNumber,birthDate,phone,gender,ruc
                test_bulk_merchant1@example.com,Pass1234!,TestBulk,MerchantUno,Demo,DNI,12345678,1988-05-15,987654321,MALE,20100000001
                test_bulk_merchant2@example.com,Pass5678!,TestBulk,MerchantDos,Demo,DNI,87654321,1992-11-20,912345678,FEMALE,20200000002
                """;
        return csvResponse(csv, "plantilla_comerciantes.csv");
    }

    @GetMapping("/template/stores")
    public ResponseEntity<byte[]> templateStores() {
        // Columnas actualizadas: se quitÃ³ colorPalette, se agregÃ³ categoryId + 3 colores individuales
        String csv = """
                storeName,categoryId,primaryColor,secondaryColor,tertiaryColor,description,merchantEmail,logoFileName
                Mi Tienda Urbana,1,ONYX_BLACK,SLATE,RAW_GOLD,Ropa urbana y accesorios,test_bulk_merchant1@example.com,MiTiendaUrbana.jpg
                Luxe Moda,1,MIDNIGHT,SAGE,COPPER,Moda premium y accesorios,test_bulk_merchant2@example.com,LuxeModa.jpg
                """;
        return csvResponse(csv, "plantilla_tiendas.csv");
    }

    // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private boolean isEmpty(MultipartFile file) {
        return file == null || file.isEmpty();
    }

    private String safeMessage(Exception e) {
        return e.getMessage() == null || e.getMessage().isBlank()
                ? "error no especificado"
                : e.getMessage();
    }

    private Map<String, Object> merchantSnapshot(Merchant merchant) {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("email", merchant.getUserAccount() != null ? merchant.getUserAccount().getEmail() : null);
        data.put("firstName", merchant.getFirstName());
        data.put("paternalSurname", merchant.getPaternalSurname());
        data.put("maternalSurname", merchant.getMaternalSurname());
        data.put("documentType", merchant.getDocumentType() != null ? merchant.getDocumentType().name() : null);
        data.put("documentNumber", merchant.getDocumentNumber());
        data.put("birthDate", merchant.getBirthDate() != null ? merchant.getBirthDate().toString() : null);
        data.put("phone", merchant.getPhone());
        data.put("gender", merchant.getGender() != null ? merchant.getGender().name() : null);
        data.put("ruc", merchant.getRuc());
        data.put("active", merchant.getActive());
        return data;
    }

    private Map<String, Object> storeSnapshot(Store store) {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("storeName", store.getStoreName());
        data.put("slug", store.getSlug());
        data.put("description", store.getDescription());
        data.put("categoryId", store.getCategory() != null ? store.getCategory().getId() : null);
        data.put("merchantEmail", store.getMerchant() != null
                && store.getMerchant().getUserAccount() != null
                ? store.getMerchant().getUserAccount().getEmail()
                : null);
        data.put("primaryColor", store.getPrimaryColor() != null ? store.getPrimaryColor().name() : null);
        data.put("secondaryColor", store.getSecondaryColor() != null ? store.getSecondaryColor().name() : null);
        data.put("tertiaryColor", store.getTertiaryColor() != null ? store.getTertiaryColor().name() : null);
        data.put("storeStatus", store.getStoreStatus() != null ? store.getStoreStatus().name() : null);
        data.put("active", store.getActive());
        return data;
    }

    private ResponseEntity<byte[]> csvResponse(String content, String filename) {
        byte[] bytes = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .contentLength(bytes.length)
                .body(bytes);
    }
}
