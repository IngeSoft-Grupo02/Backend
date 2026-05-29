package pe.edu.pucp.kingstore.service.bulk;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.kingstore.domain.dto.bulk.*;
import pe.edu.pucp.kingstore.domain.dto.store.StoreDTO;
import pe.edu.pucp.kingstore.domain.dto.user.CreateUserDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.enums.PrimaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.SecondaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.TertiaryColor;
import pe.edu.pucp.kingstore.domain.model.user.enums.DocumentType;
import pe.edu.pucp.kingstore.domain.model.user.enums.Gender;
import pe.edu.pucp.kingstore.domain.model.user.enums.Role;
import pe.edu.pucp.kingstore.repository.store.StoreCategoryRepository;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;
import pe.edu.pucp.kingstore.service.storage.StorageService;
import pe.edu.pucp.kingstore.service.store.StoreService;
import pe.edu.pucp.kingstore.service.user.UserAccountService;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class BulkUploadService {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_LOGO_SIZE_BYTES = 2L * 1024 * 1024;

    private final UserAccountService      userAccountService;
    private final StoreService            storeService;
    private final StoreRepository         storeRepository;
    private final UserAccountRepository   userAccountRepository;
    private final MerchantRepository      merchantRepository;
    private final StoreCategoryRepository categoryRepository;
    private final StorageService          storageService;

    public BulkUploadService(
            UserAccountService userAccountService,
            StoreService storeService,
            StoreRepository storeRepository,
            UserAccountRepository userAccountRepository,
            MerchantRepository merchantRepository,
            StoreCategoryRepository categoryRepository,
            StorageService storageService) {
        this.userAccountService    = userAccountService;
        this.storeService          = storeService;
        this.storeRepository       = storeRepository;
        this.userAccountRepository = userAccountRepository;
        this.merchantRepository    = merchantRepository;
        this.categoryRepository    = categoryRepository;
        this.storageService        = storageService;
    }

    // ── Punto de entrada ──────────────────────────────────────────

    @Transactional
    public BulkUploadResponseDTO process(
            MultipartFile merchantsCsv,
            MultipartFile storesCsv,
            MultipartFile logosZip) throws IOException {

        BulkUploadResponseDTO response = BulkUploadResponseDTO.builder().build();

        if (merchantsCsv != null && !merchantsCsv.isEmpty()) processMerchants(merchantsCsv, response);
        if (storesCsv    != null && !storesCsv.isEmpty())    processStores(storesCsv, response);
        if (logosZip     != null && !logosZip.isEmpty())     processLogos(logosZip, response);

        return response;
    }

    // ── Comerciantes ──────────────────────────────────────────────

    private void processMerchants(MultipartFile file, BulkUploadResponseDTO response) throws IOException {
        List<BulkMerchantRowDTO> rows = parseMerchantCsv(file);
        response.setMerchantsProcessed(rows.size());
        int created = 0;

        for (BulkMerchantRowDTO row : rows) {
            try {
                validateMerchantRow(row, file.getOriginalFilename(), response);
                boolean hasErrors = response.getIncidences().stream()
                        .anyMatch(i -> i.getBlock() == BulkIncidenceDTO.IncidenceBlock.MERCHANTS
                                && i.getRow() == row.getRowNumber()
                                && i.getType() == BulkIncidenceDTO.IncidenceType.ERROR);
                if (hasErrors) continue;

                userAccountService.createWithRole(toCreateUserDTO(row));
                created++;
            } catch (Exception e) {
                response.addIncidence(BulkIncidenceDTO.builder()
                        .block(BulkIncidenceDTO.IncidenceBlock.MERCHANTS).row(row.getRowNumber())
                        .code("UNEXPECTED").type(BulkIncidenceDTO.IncidenceType.ERROR)
                        .detail(e.getMessage()).origin(file.getOriginalFilename()).build());
            }
        }
        response.setMerchantsCreated(created);
    }

    private void validateMerchantRow(BulkMerchantRowDTO row, String filename, BulkUploadResponseDTO resp) {
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

        if (row.getPassword() == null || row.getPassword().isBlank())
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "VAL_PASSWORD", BulkIncidenceDTO.IncidenceType.ERROR, "Password es obligatorio", filename));

        if (row.getFirstName() == null || row.getFirstName().isBlank())
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "VAL_NAME", BulkIncidenceDTO.IncidenceType.ERROR, "firstName es obligatorio", filename));

        if (row.getRuc() == null || !row.getRuc().matches("\\d{11}"))
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "VAL_RUC", BulkIncidenceDTO.IncidenceType.ERROR, "RUC debe tener 11 dígitos numéricos", filename));

        if (!isValidEnum(DocumentType.class, row.getDocumentType()))
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "VAL_DOCTYPE", BulkIncidenceDTO.IncidenceType.ERROR,
                    "documentType inválido. Valores: DNI, PASSPORT, FOREIGN_ID_CARD", filename));

        if (!isValidEnum(Gender.class, row.getGender()))
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "VAL_GENDER", BulkIncidenceDTO.IncidenceType.ERROR,
                    "gender inválido. Valores: MALE, FEMALE, NOT_SPECIFIED", filename));

        try {
            if (row.getBirthDate() != null && !row.getBirthDate().isBlank())
                LocalDate.parse(row.getBirthDate());
        } catch (Exception e) {
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "VAL_DATE", BulkIncidenceDTO.IncidenceType.ERROR, "birthDate debe tener formato yyyy-MM-dd", filename));
        }
    }

    // ── Tiendas ───────────────────────────────────────────────────

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

                storeService.createFromDTO(toStoreDTO(row));
                created++;
            } catch (Exception e) {
                response.addIncidence(BulkIncidenceDTO.builder()
                        .block(BulkIncidenceDTO.IncidenceBlock.STORES).row(row.getRowNumber())
                        .code("UNEXPECTED").type(BulkIncidenceDTO.IncidenceType.ERROR)
                        .detail(e.getMessage()).origin(file.getOriginalFilename()).build());
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

        // categoryId — obligatorio y debe existir en BD
        if (row.getCategoryId() == null || row.getCategoryId().isBlank()) {
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "VAL_CATEGORY", BulkIncidenceDTO.IncidenceType.ERROR, "categoryId es obligatorio", filename));
        } else {
            try {
                Integer catId = Integer.parseInt(row.getCategoryId());
                if (categoryRepository.findById(catId).isEmpty()) {
                    resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                            "REF_NOT_FOUND", BulkIncidenceDTO.IncidenceType.ERROR,
                            "No existe categoría con ID: " + row.getCategoryId(), filename));
                }
            } catch (NumberFormatException e) {
                resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                        "VAL_CATEGORY", BulkIncidenceDTO.IncidenceType.ERROR,
                        "categoryId debe ser un número entero", filename));
            }
        }

        // primaryColor
        if (!isValidEnum(PrimaryColor.class, row.getPrimaryColor()))
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "VAL_PRIMARY_COLOR", BulkIncidenceDTO.IncidenceType.ERROR,
                    "primaryColor inválido. Valores: ONYX_BLACK, DEEP_ZINC, MIDNIGHT, CHARCOAL, ESPRESSO", filename));

        // secondaryColor
        if (!isValidEnum(SecondaryColor.class, row.getSecondaryColor()))
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "VAL_SECONDARY_COLOR", BulkIncidenceDTO.IncidenceType.ERROR,
                    "secondaryColor inválido. Valores: OLIVE_DRAB, SAGE, SLATE, TERRA, DUSTY_RED", filename));

        // tertiaryColor
        if (!isValidEnum(TertiaryColor.class, row.getTertiaryColor()))
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "VAL_TERTIARY_COLOR", BulkIncidenceDTO.IncidenceType.ERROR,
                    "tertiaryColor inválido. Valores: RICH_CAMEL, RAW_GOLD, SILVER_MIST, COPPER, STONE", filename));

        // merchantEmail — obligatorio y debe existir en BD
        if (row.getMerchantEmail() == null || row.getMerchantEmail().isBlank()) {
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "VAL_MERCHANT", BulkIncidenceDTO.IncidenceType.ERROR,
                    "merchantEmail es obligatorio", filename));
        } else if (userAccountRepository.findByEmail(row.getMerchantEmail().trim().toLowerCase()).isEmpty()) {
            resp.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "REF_NOT_FOUND", BulkIncidenceDTO.IncidenceType.ERROR,
                    "merchantEmail no existe en BD: " + row.getMerchantEmail(), filename));
        }
    }

    // ── Logos ─────────────────────────────────────────────────────

    private void processLogos(MultipartFile zipFile, BulkUploadResponseDTO response) throws IOException {
        int uploaded = 0;
        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) { zis.closeEntry(); continue; }

                String entryName = entry.getName();
                String ext = getExtension(entryName).toLowerCase();

                if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
                    response.addIncidence(BulkIncidenceDTO.builder()
                            .block(BulkIncidenceDTO.IncidenceBlock.IMAGES).row(0)
                            .code("INVALID_EXT").type(BulkIncidenceDTO.IncidenceType.WARNING)
                            .detail("Extensión no permitida: " + entryName + ". Solo: jpg, jpeg, png, webp")
                            .origin(zipFile.getOriginalFilename()).build());
                    zis.closeEntry(); continue;
                }

                byte[] bytes = zis.readAllBytes();
                if (bytes.length > MAX_LOGO_SIZE_BYTES) {
                    response.addIncidence(BulkIncidenceDTO.builder()
                            .block(BulkIncidenceDTO.IncidenceBlock.IMAGES).row(0)
                            .code("SIZE_EXCEEDED").type(BulkIncidenceDTO.IncidenceType.WARNING)
                            .detail("Imagen supera 2 MB: " + entryName)
                            .origin(zipFile.getOriginalFilename()).build());
                    zis.closeEntry(); continue;
                }

                String fileName = entryName.contains("/")
                        ? entryName.substring(entryName.lastIndexOf('/') + 1) : entryName;
                String slug = fileName.contains(".")
                        ? fileName.substring(0, fileName.lastIndexOf('.')) : fileName;

                Optional<Store> storeOpt = storeRepository.findBySlug(slug);
                if (storeOpt.isEmpty()) {
                    response.addIncidence(BulkIncidenceDTO.builder()
                            .block(BulkIncidenceDTO.IncidenceBlock.IMAGES).row(0)
                            .code("REF_NOT_FOUND").type(BulkIncidenceDTO.IncidenceType.WARNING)
                            .detail("No existe tienda con slug '" + slug + "' para imagen: " + entryName)
                            .origin(zipFile.getOriginalFilename()).build());
                    zis.closeEntry(); continue;
                }

                String s3Key = "logos/" + slug + "." + ext;
                String contentType = "image/" + (ext.equals("jpg") ? "jpeg" : ext);
                String logoUrl = storageService.uploadBytes(s3Key, bytes, contentType);

                Store store = storeOpt.get();
                store.setLogoUrl(logoUrl);
                storeRepository.save(store);
                uploaded++;
                zis.closeEntry();
            }
        }
        response.setLogosUploaded(uploaded);
    }

    // ── Parsers CSV ───────────────────────────────────────────────

    private List<BulkMerchantRowDTO> parseMerchantCsv(MultipartFile file) throws IOException {
        List<BulkMerchantRowDTO> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) return result;
            String[] headers = splitCsvLine(headerLine);
            Map<String, Integer> idx = buildIndex(headers);
            String line; int rowNum = 2;
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
            String line; int rowNum = 2;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) { rowNum++; continue; }
                String[] cols = splitCsvLine(line);
                BulkStoreRowDTO row = new BulkStoreRowDTO();
                row.setRowNumber(rowNum);
                row.setStoreName(get(cols, idx, "storeName"));
                row.setSlug(get(cols, idx, "slug"));
                row.setDescription(get(cols, idx, "description"));
                row.setCategoryId(get(cols, idx, "categoryId"));
                row.setPrimaryColor(get(cols, idx, "primaryColor"));
                row.setSecondaryColor(get(cols, idx, "secondaryColor"));
                row.setTertiaryColor(get(cols, idx, "tertiaryColor"));
                row.setMerchantEmail(get(cols, idx, "merchantEmail"));
                row.setLogoFileName(get(cols, idx, "logoFileName"));
                result.add(row);
                rowNum++;
            }
        }
        return result;
    }

    // ── Conversores ───────────────────────────────────────────────

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
        return dto;
    }

    private StoreDTO toStoreDTO(BulkStoreRowDTO row) {
        StoreDTO dto = new StoreDTO();
        dto.setStoreName(row.getStoreName());
        dto.setSlug(row.getSlug());
        dto.setDescription(row.getDescription());
        dto.setCategoryId(Integer.parseInt(row.getCategoryId()));
        dto.setPrimaryColor(PrimaryColor.valueOf(row.getPrimaryColor().toUpperCase()));
        dto.setSecondaryColor(SecondaryColor.valueOf(row.getSecondaryColor().toUpperCase()));
        dto.setTertiaryColor(TertiaryColor.valueOf(row.getTertiaryColor().toUpperCase()));

        // Resolver merchantId desde email
        if (row.getMerchantEmail() != null && !row.getMerchantEmail().isBlank()) {
            userAccountRepository.findByEmail(row.getMerchantEmail().trim().toLowerCase())
                    .flatMap(ua -> merchantRepository.findByUserAccountId(ua.getId()))
                    .ifPresent(m -> dto.setMerchantId(m.getId()));
        }
        return dto;
    }

    // ── Utilidades ────────────────────────────────────────────────

    private BulkIncidenceDTO incidence(BulkIncidenceDTO.IncidenceBlock block, int row,
                                       String code, BulkIncidenceDTO.IncidenceType type,
                                       String detail, String origin) {
        return BulkIncidenceDTO.builder()
                .block(block).row(row).code(code).type(type).detail(detail).origin(origin).build();
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
        for (int i = 0; i < headers.length; i++) idx.put(headers[i].trim(), i);
        return idx;
    }

    private String get(String[] cols, Map<String, Integer> idx, String col) {
        Integer i = idx.get(col);
        if (i == null || i >= cols.length) return null;
        String val = cols[i].trim();
        return val.isEmpty() ? null : val;
    }
}
