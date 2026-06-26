package pe.edu.pucp.kingstore.api.controller.admin;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.kingstore.domain.dto.bulk.BulkUploadResponseDTO;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;
import pe.edu.pucp.kingstore.service.bulk.BulkUploadService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/bulk")
public class BulkUploadController {

    private final BulkUploadService     bulkUploadService;
    private final UserAccountRepository userAccountRepository;
    private final StoreRepository       storeRepository;

    public BulkUploadController(
            BulkUploadService bulkUploadService,
            UserAccountRepository userAccountRepository,
            StoreRepository storeRepository) {
        this.bulkUploadService     = bulkUploadService;
        this.userAccountRepository = userAccountRepository;
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
                        .body("Debes enviar al menos un archivo (merchants, stores o logos).");
            BulkUploadResponseDTO result = bulkUploadService.process(merchantsCsv, storesCsv, logosZip);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return ResponseEntity.status(500).body("Error procesando archivos: " + e.getMessage());
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
    public ResponseEntity<Map<String, List<String>>> existingStores() {
        List<String> storeNames = storeRepository.findAll().stream()
                .map(s -> s.getStoreName().toLowerCase()).toList();
        List<String> merchantEmails = userAccountRepository.findAll().stream()
                .map(ua -> ua.getEmail().toLowerCase()).toList();
        return ResponseEntity.ok(Map.of("storeNames", storeNames, "merchantEmails", merchantEmails));
    }

    // â”€â”€ Plantillas â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @GetMapping("/template/merchants")
    public ResponseEntity<byte[]> templateMerchants() {
        String csv = """
                email,password,firstName,paternalSurname,maternalSurname,documentType,documentNumber,birthDate,phone,gender,ruc
                juan.perez@ejemplo.com,Pass1234!,Juan,Perez,Garcia,DNI,12345678,1988-05-15,987654321,MALE,20100000001
                maria.torres@ejemplo.com,Pass5678!,Maria,Torres,Lopez,DNI,87654321,1992-11-20,912345678,FEMALE,20200000002
                """;
        return csvResponse(csv, "plantilla_comerciantes.csv");
    }

    @GetMapping("/template/stores")
    public ResponseEntity<byte[]> templateStores() {
        // Columnas actualizadas: se quitÃ³ colorPalette, se agregÃ³ categoryId + 3 colores individuales
        String csv = """
                storeName,categoryId,primaryColor,secondaryColor,tertiaryColor,description,merchantEmail,logoFileName
                Mi Tienda Urbana,1,ONYX_BLACK,SLATE,RAW_GOLD,Ropa urbana para jovenes,juan.perez@ejemplo.com,MiTiendaUrbana.png
                Luxe Moda,2,MIDNIGHT,SAGE,RAW_GOLD,Alta costura accesible,maria.torres@ejemplo.com,
                """;
        return csvResponse(csv, "plantilla_tiendas.csv");
    }

    // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private boolean isEmpty(MultipartFile file) {
        return file == null || file.isEmpty();
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
