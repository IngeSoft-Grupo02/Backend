package pe.edu.pucp.kingstore.service.bulk;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.kingstore.domain.dto.bulk.*;
import pe.edu.pucp.kingstore.domain.dto.user.CreateUserDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.enums.ColorPalette;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.domain.model.user.enums.DocumentType;
import pe.edu.pucp.kingstore.domain.model.user.enums.Gender;
import pe.edu.pucp.kingstore.domain.model.user.enums.Role;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;
import pe.edu.pucp.kingstore.service.storage.StorageService;
import pe.edu.pucp.kingstore.service.store.StoreService;
import pe.edu.pucp.kingstore.domain.dto.store.StoreDTO;
import pe.edu.pucp.kingstore.service.user.UserAccountService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Orquesta la carga masiva de comerciantes, tiendas y logos.
 *
 * Flujo:
 *   1. Parsear CSV de comerciantes → validar → persistir
 *   2. Parsear CSV de tiendas     → validar → persistir
 *   3. Descomprimir ZIP de logos  → subir a S3 → actualizar store.logoUrl
 *
 * Si una fila falla, se registra una incidencia y se continúa con el resto
 * (fail-per-row, no fail-fast), para que el admin vea todos los errores juntos.
 */
@Service
public class BulkUploadService {

    // ── Extensiones de imagen permitidas ──────────────────────────
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_LOGO_SIZE_BYTES = 2L * 1024 * 1024; // 2 MB

    private final UserAccountService userAccountService;
    private final StoreService storeService;
    private final StoreRepository storeRepository;
    private final UserAccountRepository userAccountRepository;
    private final MerchantRepository merchantRepository;
    private final StorageService storageService;

    public BulkUploadService(
            UserAccountService userAccountService,
            StoreService storeService,
            StoreRepository storeRepository,
            UserAccountRepository userAccountRepository,
            MerchantRepository merchantRepository,
            StorageService storageService) {
        this.userAccountService = userAccountService;
        this.storeService = storeService;
        this.storeRepository = storeRepository;
        this.userAccountRepository = userAccountRepository;
        this.merchantRepository = merchantRepository;
        this.storageService = storageService;
    }

    // ═══════════════════════════════════════════════════════════════
    //  PUNTO DE ENTRADA PRINCIPAL
    // ═══════════════════════════════════════════════════════════════

    @Transactional
    public BulkUploadResponseDTO process(
            MultipartFile merchantsCsv,
            MultipartFile storesCsv,
            MultipartFile logosZip) throws IOException {

        BulkUploadResponseDTO response = BulkUploadResponseDTO.builder().build();

        if (merchantsCsv != null && !merchantsCsv.isEmpty()) {
            processMerchants(merchantsCsv, response);
        }

        if (storesCsv != null && !storesCsv.isEmpty()) {
            processStores(storesCsv, response);
        }

        // Pasar storesCsv para resolver logoFileName → slug
        if (logosZip != null && !logosZip.isEmpty()) {
            processLogos(logosZip, storesCsv, response);
        }

        return response;
    }

    // ═══════════════════════════════════════════════════════════════
    //  COMERCIANTES
    // ═══════════════════════════════════════════════════════════════

    private void processMerchants(MultipartFile file, BulkUploadResponseDTO response) throws IOException {
        List<BulkMerchantRowDTO> rows = parseMerchantCsv(file);
        response.setMerchantsProcessed(rows.size());
        int created = 0;

        for (BulkMerchantRowDTO row : rows) {
            try {
                // Validaciones
                validateMerchantRow(row, file.getOriginalFilename(), response);
                boolean hasErrors = response.getIncidences().stream()
                        .anyMatch(i -> i.getBlock() == BulkIncidenceDTO.IncidenceBlock.MERCHANTS
                                && i.getRow() == row.getRowNumber()
                                && i.getType() == BulkIncidenceDTO.IncidenceType.ERROR);
                if (hasErrors) continue;

                // Crear via servicio existente
                CreateUserDTO dto = toCreateUserDTO(row);
                userAccountService.createWithRole(dto);
                created++;

            } catch (Exception e) {
                response.addIncidence(BulkIncidenceDTO.builder()
                        .block(BulkIncidenceDTO.IncidenceBlock.MERCHANTS)
                        .row(row.getRowNumber())
                        .code("UNEXPECTED")
                        .type(BulkIncidenceDTO.IncidenceType.ERROR)
                        .detail(e.getMessage())
                        .origin(file.getOriginalFilename())
                        .build());
            }
        }
        response.setMerchantsCreated(created);
    }

    private void validateMerchantRow(BulkMerchantRowDTO row, String filename, BulkUploadResponseDTO resp) {
        // email
        if (row.getEmail() == null || row.getEmail().isBlank()) {
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "VAL_EMAIL", BulkIncidenceDTO.IncidenceType.ERROR, "Email es obligatorio", filename));
        } else if (!row.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "VAL_EMAIL", BulkIncidenceDTO.IncidenceType.ERROR, "Formato de email inválido: " + row.getEmail(), filename));
        } else if (userAccountRepository.findByEmail(row.getEmail().trim().toLowerCase()).isPresent()) {
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "DUPLICATE", BulkIncidenceDTO.IncidenceType.ERROR, "Email ya registrado: " + row.getEmail(), filename));
        }

        // password
        if (row.getPassword() == null || row.getPassword().isBlank()) {
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "VAL_PASSWORD", BulkIncidenceDTO.IncidenceType.ERROR, "Password es obligatorio", filename));
        }

        // nombre
        if (row.getFirstName() == null || row.getFirstName().isBlank()) {
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "VAL_NAME", BulkIncidenceDTO.IncidenceType.ERROR, "firstName es obligatorio", filename));
        }

        // RUC (11 dígitos)
        if (row.getRuc() == null || !row.getRuc().matches("\\d{11}")) {
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "VAL_RUC", BulkIncidenceDTO.IncidenceType.ERROR, "RUC debe tener 11 dígitos numéricos", filename));
        }

        // documentType
        if (!isValidEnum(DocumentType.class, row.getDocumentType())) {
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "VAL_DOCTYPE", BulkIncidenceDTO.IncidenceType.ERROR,
                    "documentType inválido. Valores: DNI, PASSPORT, FOREIGN_ID_CARD", filename));
        }

        // gender
        if (!isValidEnum(Gender.class, row.getGender())) {
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "VAL_GENDER", BulkIncidenceDTO.IncidenceType.ERROR,
                    "gender inválido. Valores: MALE, FEMALE, NOT_SPECIFIED", filename));
        }

        // birthDate
        try {
            if (row.getBirthDate() != null && !row.getBirthDate().isBlank())
                LocalDate.parse(row.getBirthDate());
        } catch (Exception e) {
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "VAL_DATE", BulkIncidenceDTO.IncidenceType.ERROR, "birthDate debe tener formato yyyy-MM-dd", filename));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  TIENDAS
    // ═══════════════════════════════════════════════════════════════

    private void processStores(MultipartFile file, BulkUploadResponseDTO response) throws IOException {
        List<BulkStoreRowDTO> rows = parseStoreCsv(file);
        response.setStoresProcessed(rows.size());
        int created = 0;

        for (BulkStoreRowDTO row : rows) {
            try {
                validateStoreRow(row, file.getOriginalFilename(), response);
                boolean hasErrors = response.getIncidences().stream()
                        .anyMatch(i -> i.getBlock() == BulkIncidenceDTO.IncidenceBlock.STORES
                                && i.getRow() == row.getRowNumber()
                                && i.getType() == BulkIncidenceDTO.IncidenceType.ERROR);
                if (hasErrors) continue;

                StoreDTO dto = toStoreDTO(row);
                storeService.createFromDTO(dto);
                created++;

            } catch (Exception e) {
                response.addIncidence(BulkIncidenceDTO.builder()
                        .block(BulkIncidenceDTO.IncidenceBlock.STORES)
                        .row(row.getRowNumber())
                        .code("UNEXPECTED")
                        .type(BulkIncidenceDTO.IncidenceType.ERROR)
                        .detail(e.getMessage())
                        .origin(file.getOriginalFilename())
                        .build());
            }
        }
        response.setStoresCreated(created);
    }

    private void validateStoreRow(BulkStoreRowDTO row, String filename, BulkUploadResponseDTO resp) {
        // storeName
        if (row.getStoreName() == null || row.getStoreName().isBlank()) {
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "VAL_NAME", BulkIncidenceDTO.IncidenceType.ERROR, "storeName es obligatorio", filename));
        } else if (row.getStoreName().length() > 100) {
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "VAL_NAME", BulkIncidenceDTO.IncidenceType.ERROR, "storeName supera 100 caracteres", filename));
        }

        // slug
        if (row.getSlug() == null || row.getSlug().isBlank()) {
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "VAL_SLUG", BulkIncidenceDTO.IncidenceType.ERROR, "slug es obligatorio", filename));
        } else if (storeRepository.findBySlug(row.getSlug().trim().toLowerCase()).isPresent()) {
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "DUPLICATE", BulkIncidenceDTO.IncidenceType.ERROR, "Slug ya registrado: " + row.getSlug(), filename));
        }

        // colorPalette
        if (!isValidEnum(ColorPalette.class, row.getColorPalette())) {
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "VAL_PALETTE", BulkIncidenceDTO.IncidenceType.ERROR,
                    "colorPalette inválido. Valores: CORESTREET, ATELIERMONO, UTILITYDROP, LUXECAPSULE", filename));
        }

        // merchantEmail (opcional, pero si viene debe existir)
        if (row.getMerchantEmail() != null && !row.getMerchantEmail().isBlank()) {
            boolean exists = userAccountRepository.findByEmail(row.getMerchantEmail().trim().toLowerCase()).isPresent();
            if (!exists) {
                resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                        "REF_NOT_FOUND", BulkIncidenceDTO.IncidenceType.WARNING,
                        "merchantEmail no existe en BD: " + row.getMerchantEmail() + ". La tienda se creará sin comerciante asignado.", filename));
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  LOGOS (ZIP → S3)
    // ═══════════════════════════════════════════════════════════════

    private void processLogos(MultipartFile zipFile, MultipartFile storesCsv,
                              BulkUploadResponseDTO response) throws IOException {
        int uploaded = 0;

        // Construir mapa: logoFileName → slug de tienda (desde el CSV de tiendas)
        Map<String, String> logoToSlug = new HashMap<>();
        if (storesCsv != null && !storesCsv.isEmpty()) {
            List<BulkStoreRowDTO> storeRows = parseStoreCsv(storesCsv);
            for (BulkStoreRowDTO row : storeRows) {
                if (row.getLogoFileName() != null && !row.getLogoFileName().isBlank()
                        && row.getSlug() != null && !row.getSlug().isBlank()) {
                    logoToSlug.put(row.getLogoFileName().trim().toLowerCase(),
                            row.getSlug().trim().toLowerCase());
                }
            }
        }

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) { zis.closeEntry(); continue; }

                String entryName = entry.getName();
                // Solo el nombre del archivo, sin path
                String fileName = entryName.contains("/")
                        ? entryName.substring(entryName.lastIndexOf('/') + 1)
                        : entryName;
                String ext = getExtension(fileName).toLowerCase();

                // Validar extensión
                if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
                    response.addIncidence(BulkIncidenceDTO.builder()
                            .block(BulkIncidenceDTO.IncidenceBlock.IMAGES)
                            .row(0).code("INVALID_EXT")
                            .type(BulkIncidenceDTO.IncidenceType.WARNING)
                            .detail("Extensión no permitida: " + fileName + ". Solo: jpg, jpeg, png, webp")
                            .origin(zipFile.getOriginalFilename())
                            .build());
                    zis.closeEntry();
                    continue;
                }

                byte[] bytes = zis.readAllBytes();

                // Validar tamaño (2 MB)
                if (bytes.length > MAX_LOGO_SIZE_BYTES) {
                    response.addIncidence(BulkIncidenceDTO.builder()
                            .block(BulkIncidenceDTO.IncidenceBlock.IMAGES)
                            .row(0).code("SIZE_EXCEEDED")
                            .type(BulkIncidenceDTO.IncidenceType.WARNING)
                            .detail("Imagen supera 2 MB: " + fileName)
                            .origin(zipFile.getOriginalFilename())
                            .build());
                    zis.closeEntry();
                    continue;
                }

                // Buscar slug de tienda via mapa logoFileName → slug
                String slug = logoToSlug.get(fileName.toLowerCase());
                if (slug == null) {
                    response.addIncidence(BulkIncidenceDTO.builder()
                            .block(BulkIncidenceDTO.IncidenceBlock.IMAGES)
                            .row(0).code("REF_NOT_FOUND")
                            .type(BulkIncidenceDTO.IncidenceType.WARNING)
                            .detail("El archivo \"" + fileName + "\" no está referenciado en ninguna fila del CSV de tiendas (columna logoFileName).")
                            .origin(zipFile.getOriginalFilename())
                            .build());
                    zis.closeEntry();
                    continue;
                }

                // Buscar tienda por slug
                Optional<Store> storeOpt = storeRepository.findBySlug(slug);
                if (storeOpt.isEmpty()) {
                    response.addIncidence(BulkIncidenceDTO.builder()
                            .block(BulkIncidenceDTO.IncidenceBlock.IMAGES)
                            .row(0).code("REF_NOT_FOUND")
                            .type(BulkIncidenceDTO.IncidenceType.WARNING)
                            .detail("No existe tienda con slug '" + slug + "' para imagen: " + fileName)
                            .origin(zipFile.getOriginalFilename())
                            .build());
                    zis.closeEntry();
                    continue;
                }

                // Subir a storage (local o S3 según perfil)
                String s3Key = "logos/" + slug + "." + ext;
                String contentType = "image/" + (ext.equals("jpg") ? "jpeg" : ext);
                String logoUrl = this.storageService.uploadBytes(s3Key, bytes, contentType);

                // Actualizar tienda
                Store store = storeOpt.get();
                store.setLogoUrl(logoUrl);
                storeRepository.save(store);
                uploaded++;

                zis.closeEntry();
            }
        }

        response.setLogosUploaded(uploaded);
    }

    // ═══════════════════════════════════════════════════════════════
    //  PARSERS CSV
    // ═══════════════════════════════════════════════════════════════

    /**
     * Parsea CSV simple (separado por comas, primera fila = cabecera).
     * Soporta comillas dobles para campos con comas internas.
     */
    private List<BulkMerchantRowDTO> parseMerchantCsv(MultipartFile file) throws IOException {
        List<BulkMerchantRowDTO> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) return result;

            String[] headers = splitCsvLine(headerLine);
            Map<String, Integer> idx = buildIndex(headers);

            String line;
            int rowNum = 2; // fila 1 = cabecera
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) { rowNum++; continue; }
                String[] cols = splitCsvLine(line);
                BulkMerchantRowDTO row = new BulkMerchantRowDTO();
                row.setRowNumber(rowNum);
                row.setEmail(get(cols, idx, "email"));
                row.setPassword(get(cols, idx, "password"));
                row.setFirstName(get(cols, idx, "firstName"));
                row.setPaternalSurname(get(cols, idx, "paternalSurname"));
                row.setMaternalSurname(get(cols, idx, "maternalSurname"));
                row.setDocumentType(get(cols, idx, "documentType"));
                row.setDocumentNumber(get(cols, idx, "documentNumber"));
                row.setBirthDate(get(cols, idx, "birthDate"));
                row.setPhone(get(cols, idx, "phone"));
                row.setGender(get(cols, idx, "gender"));
                row.setRuc(get(cols, idx, "ruc"));
                row.setStoreId(get(cols, idx, "storeId"));
                result.add(row);
                rowNum++;
            }
        }
        return result;
    }

    private List<BulkStoreRowDTO> parseStoreCsv(MultipartFile file) throws IOException {
        List<BulkStoreRowDTO> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) return result;

            String[] headers = splitCsvLine(headerLine);
            Map<String, Integer> idx = buildIndex(headers);

            String line;
            int rowNum = 2;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) { rowNum++; continue; }
                String[] cols = splitCsvLine(line);
                BulkStoreRowDTO row = new BulkStoreRowDTO();
                row.setRowNumber(rowNum);
                row.setStoreName(get(cols, idx, "storeName"));
                row.setSlug(get(cols, idx, "slug"));
                row.setColorPalette(get(cols, idx, "colorPalette"));
                row.setDescription(get(cols, idx, "description"));
                row.setMerchantEmail(get(cols, idx, "merchantEmail"));
                row.setLogoFileName(get(cols, idx, "logoFileName"));
                result.add(row);
                rowNum++;
            }
        }
        return result;
    }

    // ═══════════════════════════════════════════════════════════════
    //  CONVERSORES DTO → MODELO
    // ═══════════════════════════════════════════════════════════════

    private CreateUserDTO toCreateUserDTO(BulkMerchantRowDTO row) {
        CreateUserDTO dto = new CreateUserDTO();
        dto.setEmail(row.getEmail());
        dto.setPassword(row.getPassword());
        dto.setFirstName(row.getFirstName());
        dto.setPaternalSurname(row.getPaternalSurname() != null ? row.getPaternalSurname() : "");
        dto.setMaternalSurname(row.getMaternalSurname() != null ? row.getMaternalSurname() : "");
        dto.setDocumentType(DocumentType.valueOf(row.getDocumentType().toUpperCase()));
        dto.setDocumentNumber(row.getDocumentNumber());
        dto.setBirthDate(row.getBirthDate() != null && !row.getBirthDate().isBlank()
                ? LocalDate.parse(row.getBirthDate()) : null);
        dto.setPhone(row.getPhone());
        dto.setGender(Gender.valueOf(row.getGender().toUpperCase()));
        dto.setRuc(row.getRuc());
        dto.setRole(Role.MERCHANT);
        if (row.getStoreId() != null && !row.getStoreId().isBlank()) {
            try { dto.setStoreId(Integer.parseInt(row.getStoreId())); } catch (NumberFormatException ignored) {}
        }
        return dto;
    }

    private StoreDTO toStoreDTO(BulkStoreRowDTO row) {
        StoreDTO dto = new StoreDTO();
        dto.setStoreName(row.getStoreName());
        dto.setSlug(row.getSlug());
        dto.setDescription(row.getDescription());
        dto.setColorPalette(ColorPalette.valueOf(row.getColorPalette().toUpperCase()));

        // Resolver merchantEmail → merchantId
        if (row.getMerchantEmail() != null && !row.getMerchantEmail().isBlank()) {
            merchantRepository
                    .findByUserAccount_Email(row.getMerchantEmail().trim().toLowerCase())
                    .ifPresent(merchant -> dto.setMerchantId(merchant.getId()));
        }

        return dto;
    }

    // ═══════════════════════════════════════════════════════════════
    //  UTILIDADES
    // ═══════════════════════════════════════════════════════════════

    private BulkIncidenceDTO incidence(BulkIncidenceDTO.IncidenceBlock block, int row,
                                        String code, BulkIncidenceDTO.IncidenceType type,
                                        String detail, String origin) {
        return BulkIncidenceDTO.builder()
                .block(block).row(row).code(code).type(type).detail(detail).origin(origin)
                .build();
    }

    private <E extends Enum<E>> boolean isValidEnum(Class<E> clazz, String value) {
        if (value == null || value.isBlank()) return false;
        try { Enum.valueOf(clazz, value.toUpperCase()); return true; }
        catch (IllegalArgumentException e) { return false; }
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1) : "";
    }

    private String[] splitCsvLine(String line) {
        // Separación simple por coma respetando comillas
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '"') { inQuotes = !inQuotes; }
            else if (c == ',' && !inQuotes) { tokens.add(current.toString().trim()); current = new StringBuilder(); }
            else { current.append(c); }
        }
        tokens.add(current.toString().trim());
        return tokens.toArray(new String[0]);
    }

    private Map<String, Integer> buildIndex(String[] headers) {
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            idx.put(headers[i].trim(), i);
        }
        return idx;
    }

    private String get(String[] cols, Map<String, Integer> idx, String col) {
        Integer i = idx.get(col);
        if (i == null || i >= cols.length) return null;
        String val = cols[i].trim();
        return val.isEmpty() ? null : val;
    }
}
