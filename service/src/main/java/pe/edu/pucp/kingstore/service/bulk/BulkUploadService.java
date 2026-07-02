package pe.edu.pucp.kingstore.service.bulk;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.kingstore.domain.dto.bulk.BulkIncidenceDTO;
import pe.edu.pucp.kingstore.domain.dto.bulk.BulkMerchantRowDTO;
import pe.edu.pucp.kingstore.domain.dto.bulk.BulkStoreRowDTO;
import pe.edu.pucp.kingstore.domain.dto.bulk.BulkUploadResponseDTO;
import pe.edu.pucp.kingstore.domain.dto.store.StoreDTO;
import pe.edu.pucp.kingstore.domain.dto.user.CreateUserDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.enums.PrimaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.SecondaryColor;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.domain.model.store.enums.TertiaryColor;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
import pe.edu.pucp.kingstore.domain.model.user.enums.DocumentType;
import pe.edu.pucp.kingstore.domain.model.user.enums.Gender;
import pe.edu.pucp.kingstore.domain.model.user.enums.Role;
import pe.edu.pucp.kingstore.repository.store.StoreCategoryRepository;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;
import pe.edu.pucp.kingstore.service.storage.StorageService;
import pe.edu.pucp.kingstore.service.store.StoreService;
import pe.edu.pucp.kingstore.service.store.StoreSlugUtil;
import pe.edu.pucp.kingstore.service.user.PasswordHashService;
import pe.edu.pucp.kingstore.service.user.UserAccountService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class BulkUploadService {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_LOGO_SIZE_BYTES = 2L * 1024 * 1024;

    private final UserAccountService userAccountService;
    private final StoreService storeService;
    private final StoreRepository storeRepository;
    private final UserAccountRepository userAccountRepository;
    private final MerchantRepository merchantRepository;
    private final StoreCategoryRepository categoryRepository;
    private final StorageService storageService;
    private final PasswordHashService passwordHashService = new PasswordHashService();

    public BulkUploadService(
            UserAccountService userAccountService,
            StoreService storeService,
            StoreRepository storeRepository,
            UserAccountRepository userAccountRepository,
            MerchantRepository merchantRepository,
            StoreCategoryRepository categoryRepository,
            StorageService storageService) {
        this.userAccountService = userAccountService;
        this.storeService = storeService;
        this.storeRepository = storeRepository;
        this.userAccountRepository = userAccountRepository;
        this.merchantRepository = merchantRepository;
        this.categoryRepository = categoryRepository;
        this.storageService = storageService;
    }

    @Transactional
    public BulkUploadResponseDTO process(
            MultipartFile merchantsCsv,
            MultipartFile storesCsv,
            MultipartFile logosZip) throws IOException {

        BulkUploadResponseDTO response = BulkUploadResponseDTO.builder().build();
        List<BulkMerchantRowDTO> merchantRows = merchantsCsv != null && !merchantsCsv.isEmpty()
                ? parseMerchantCsv(merchantsCsv)
                : List.of();
        List<BulkStoreRowDTO> storeRows = storesCsv != null && !storesCsv.isEmpty()
                ? parseStoreCsv(storesCsv)
                : List.of();
        Map<String, String> logoEntryNames = logosZip != null && !logosZip.isEmpty()
                ? collectLogoEntryNames(logosZip)
                : Map.of();

        response.setMerchantsProcessed(merchantRows.size());
        response.setStoresProcessed(storeRows.size());

        ValidationContext context = new ValidationContext();
        validateMerchants(
                merchantRows,
                merchantsCsv != null ? merchantsCsv.getOriginalFilename() : null,
                response,
                context);
        validateStores(
                storeRows,
                storesCsv != null ? storesCsv.getOriginalFilename() : null,
                response,
                context,
                logosZip != null && !logosZip.isEmpty(),
                logoEntryNames);

        if (response.getErrorCount() > 0) {
            return response;
        }

        createMerchants(merchantRows, response, context);
        Map<String, Store> storesByLogoFileName = createStores(storeRows, response, context);

        if (logosZip != null && !logosZip.isEmpty()) {
            processLogos(logosZip, response, storesByLogoFileName, storeRows.isEmpty());
        }

        return response;
    }

    private void validateMerchants(List<BulkMerchantRowDTO> rows, String filename,
                                   BulkUploadResponseDTO response, ValidationContext context) {
        for (BulkMerchantRowDTO row : rows) {
            validateMerchantRow(row, filename, response, context);
        }
    }

    private void validateMerchantRow(BulkMerchantRowDTO row, String filename,
                                     BulkUploadResponseDTO response, ValidationContext context) {
        int errorsBefore = response.getErrorCount();
        String email = normalizeEmail(row.getEmail());

        if (email == null) {
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "VAL_EMAIL", BulkIncidenceDTO.IncidenceType.ERROR, "email es obligatorio", filename));
        } else if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "VAL_EMAIL", BulkIncidenceDTO.IncidenceType.ERROR,
                    "Formato de email invalido: " + row.getEmail(), filename));
        } else if (!context.seenMerchantEmails.add(email)) {
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "DUPLICATE", BulkIncidenceDTO.IncidenceType.ERROR,
                    "Email duplicado en archivo: " + row.getEmail(), filename));
        }

        requireText(response, BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                "VAL_PASSWORD", row.getPassword(), "password es obligatorio", filename);
        requireText(response, BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                "VAL_NAME", row.getFirstName(), "firstName es obligatorio", filename);
        requireText(response, BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                "VAL_NAME", row.getPaternalSurname(), "paternalSurname es obligatorio", filename);
        requireText(response, BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                "VAL_NAME", row.getMaternalSurname(), "maternalSurname es obligatorio", filename);
        requireText(response, BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                "VAL_DOCUMENT", row.getDocumentNumber(), "documentNumber es obligatorio", filename);

        if (row.getRuc() == null || !row.getRuc().matches("\\d{11}")) {
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "VAL_RUC", BulkIncidenceDTO.IncidenceType.ERROR,
                    "ruc debe tener 11 digitos numericos", filename));
        }

        if (!isValidEnum(DocumentType.class, row.getDocumentType())) {
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "VAL_DOCTYPE", BulkIncidenceDTO.IncidenceType.ERROR,
                    "documentType invalido. Valores: DNI, PASSPORT, FOREIGN_ID_CARD", filename));
        }

        if (!isValidEnum(Gender.class, row.getGender())) {
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "VAL_GENDER", BulkIncidenceDTO.IncidenceType.ERROR,
                    "gender invalido. Valores: MALE, FEMALE, NOT_SPECIFIED", filename));
        }

        if (row.getBirthDate() == null || row.getBirthDate().isBlank()) {
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "VAL_DATE", BulkIncidenceDTO.IncidenceType.ERROR,
                    "birthDate es obligatorio", filename));
        } else {
            try {
                LocalDate.parse(row.getBirthDate());
            } catch (Exception e) {
                response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                        "VAL_DATE", BulkIncidenceDTO.IncidenceType.ERROR,
                        "birthDate debe tener formato yyyy-MM-dd", filename));
            }
        }

        if (response.getErrorCount() > errorsBefore) {
            if (email != null) {
                context.invalidMerchantEmails.add(email);
            }
            return;
        }

        Optional<UserAccount> existingAccount = userAccountRepository.findByEmail(email);
        if (existingAccount.isEmpty()) {
            context.validMerchantEmails.add(email);
            context.newMerchantEmails.add(email);
            return;
        }

        Optional<Merchant> existingMerchant = merchantRepository.findByUserAccountId(existingAccount.get().getId());
        if (existingMerchant.isPresent() && merchantMatches(row, existingAccount.get(), existingMerchant.get())) {
            context.validMerchantEmails.add(email);
            context.existingMerchantEmails.add(email);
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "SKIPPED_EXISTING", BulkIncidenceDTO.IncidenceType.WARNING,
                    "Comerciante ya existente omitido: " + email, filename));
        } else {
            context.invalidMerchantEmails.add(email);
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.MERCHANTS, row.getRowNumber(),
                    "CONFLICT", BulkIncidenceDTO.IncidenceType.ERROR,
                    "Comerciante existente con datos distintos o sin perfil MERCHANT: " + email, filename));
        }
    }

    private void createMerchants(List<BulkMerchantRowDTO> rows, BulkUploadResponseDTO response,
                                 ValidationContext context) {
        int created = 0;
        for (BulkMerchantRowDTO row : rows) {
            String email = normalizeEmail(row.getEmail());
            if (!context.newMerchantEmails.contains(email)) {
                continue;
            }
            userAccountService.createWithRole(toCreateUserDTO(row));
            created++;
        }
        response.setMerchantsCreated(created);
    }

    private void validateStores(List<BulkStoreRowDTO> rows, String filename,
                                BulkUploadResponseDTO response, ValidationContext context,
                                boolean hasLogosZip, Map<String, String> logoEntryNames) {
        for (BulkStoreRowDTO row : rows) {
            validateStoreRow(row, filename, response, context, hasLogosZip, logoEntryNames);
        }

        if (hasLogosZip && !rows.isEmpty() && response.getErrorCount() == 0) {
            for (Map.Entry<String, String> entry : logoEntryNames.entrySet()) {
                if (!context.referencedLogos.containsKey(entry.getKey())) {
                    response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.IMAGES, 0,
                            "UNREFERENCED", BulkIncidenceDTO.IncidenceType.WARNING,
                            "Logo no referenciado por una tienda nueva: " + entry.getValue(), null));
                }
            }
        }
    }

    private void validateStoreRow(BulkStoreRowDTO row, String filename, BulkUploadResponseDTO response,
                                  ValidationContext context, boolean hasLogosZip,
                                  Map<String, String> logoEntryNames) {
        int errorsBefore = response.getErrorCount();
        String slug = storeSlug(row.getStoreName());

        if (row.getStoreName() == null || row.getStoreName().isBlank()) {
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "VAL_NAME", BulkIncidenceDTO.IncidenceType.ERROR, "storeName es obligatorio", filename));
        } else if (row.getStoreName().length() > 100) {
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "VAL_NAME", BulkIncidenceDTO.IncidenceType.ERROR, "storeName supera 100 caracteres", filename));
        } else if (!context.seenStoreSlugs.add(slug)) {
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "DUPLICATE", BulkIncidenceDTO.IncidenceType.ERROR,
                    "Tienda duplicada en archivo: " + row.getStoreName(), filename));
        }

        validateCategory(row, filename, response);
        validateColors(row, filename, response);
        validateMerchantReference(row, filename, response, context);
        validateLogoReference(row, filename, response, hasLogosZip, logoEntryNames);

        Optional<Store> existingStore = slug == null ? Optional.empty() : storeRepository.findBySlug(slug);
        if (existingStore.isPresent()) {
            if (storeMatches(row, existingStore.get())) {
                context.skippedStoreSlugs.add(slug);
                response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                        "SKIPPED_EXISTING", BulkIncidenceDTO.IncidenceType.WARNING,
                        "Tienda ya existente omitida: " + row.getStoreName(), filename));
            } else {
                context.invalidStoreSlugs.add(slug);
                response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                        "CONFLICT", BulkIncidenceDTO.IncidenceType.ERROR,
                        "Tienda existente con datos distintos: " + row.getStoreName(), filename));
            }
        }

        if (response.getErrorCount() > errorsBefore) {
            if (slug != null) {
                context.invalidStoreSlugs.add(slug);
            }
            return;
        }

        if (slug != null && !context.skippedStoreSlugs.contains(slug)) {
            context.newStoreSlugs.add(slug);
            if (row.getLogoFileName() != null && !row.getLogoFileName().isBlank()) {
                context.referencedLogos.put(normalizeFileName(row.getLogoFileName()), baseName(row.getLogoFileName()));
            }
        }
    }

    private void validateCategory(BulkStoreRowDTO row, String filename, BulkUploadResponseDTO response) {
        if (row.getCategoryId() == null || row.getCategoryId().isBlank()) {
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "VAL_CATEGORY", BulkIncidenceDTO.IncidenceType.ERROR, "categoryId es obligatorio", filename));
            return;
        }
        try {
            Integer catId = Integer.parseInt(row.getCategoryId());
            if (categoryRepository.findById(catId).isEmpty()) {
                response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                        "REF_NOT_FOUND", BulkIncidenceDTO.IncidenceType.ERROR,
                        "No existe categoria con ID: " + row.getCategoryId(), filename));
            }
        } catch (NumberFormatException e) {
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "VAL_CATEGORY", BulkIncidenceDTO.IncidenceType.ERROR,
                    "categoryId debe ser un numero entero", filename));
        }
    }

    private void validateColors(BulkStoreRowDTO row, String filename, BulkUploadResponseDTO response) {
        if (!isValidEnum(PrimaryColor.class, row.getPrimaryColor())) {
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "VAL_COLOR", BulkIncidenceDTO.IncidenceType.ERROR,
                    invalidColorMessage("primaryColor", row.getPrimaryColor(), PrimaryColor.class), filename));
        }
        if (!isValidEnum(SecondaryColor.class, row.getSecondaryColor())) {
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "VAL_COLOR", BulkIncidenceDTO.IncidenceType.ERROR,
                    invalidColorMessage("secondaryColor", row.getSecondaryColor(), SecondaryColor.class), filename));
        }
        if (!isValidEnum(TertiaryColor.class, row.getTertiaryColor())) {
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "VAL_COLOR", BulkIncidenceDTO.IncidenceType.ERROR,
                    invalidColorMessage("tertiaryColor", row.getTertiaryColor(), TertiaryColor.class), filename));
        }
    }

    private void validateMerchantReference(BulkStoreRowDTO row, String filename,
                                           BulkUploadResponseDTO response, ValidationContext context) {
        String merchantEmail = normalizeEmail(row.getMerchantEmail());
        if (merchantEmail == null) {
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "VAL_MERCHANT", BulkIncidenceDTO.IncidenceType.ERROR,
                    "merchantEmail es obligatorio", filename));
            return;
        }
        if (context.validMerchantEmails.contains(merchantEmail)) {
            return;
        }
        if (context.invalidMerchantEmails.contains(merchantEmail)) {
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "REF_INVALID", BulkIncidenceDTO.IncidenceType.ERROR,
                    "merchantEmail referencia un comerciante invalido dentro del lote: " + merchantEmail, filename));
            return;
        }

        Optional<UserAccount> account = userAccountRepository.findByEmail(merchantEmail);
        if (account.isEmpty()) {
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "REF_NOT_FOUND", BulkIncidenceDTO.IncidenceType.ERROR,
                    "merchantEmail no existe en BD ni en comerciantes validos del lote: " + merchantEmail, filename));
            return;
        }

        Optional<Merchant> merchant = merchantRepository.findByUserAccountId(account.get().getId());
        if (merchant.isEmpty()) {
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "REF_NOT_FOUND", BulkIncidenceDTO.IncidenceType.ERROR,
                    "merchantEmail existe, pero no pertenece a un comerciante: " + merchantEmail, filename));
            return;
        }

        context.validMerchantEmails.add(merchantEmail);
        context.existingMerchantEmails.add(merchantEmail);
    }

    private void validateLogoReference(BulkStoreRowDTO row, String filename, BulkUploadResponseDTO response,
                                       boolean hasLogosZip, Map<String, String> logoEntryNames) {
        if (row.getLogoFileName() == null || row.getLogoFileName().isBlank()) {
            return;
        }
        String normalizedLogo = normalizeFileName(row.getLogoFileName());
        if (!hasLogosZip) {
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "VAL_LOGO", BulkIncidenceDTO.IncidenceType.ERROR,
                    "logoFileName referencia un archivo, pero no se subio ZIP de logos: "
                            + row.getLogoFileName(), filename));
            return;
        }
        if (!logoEntryNames.containsKey(normalizedLogo)) {
            response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.STORES, row.getRowNumber(),
                    "REF_NOT_FOUND", BulkIncidenceDTO.IncidenceType.ERROR,
                    "logoFileName no existe en el ZIP por nombre de archivo: "
                            + row.getLogoFileName(), filename));
        }
    }

    private Map<String, Store> createStores(List<BulkStoreRowDTO> rows, BulkUploadResponseDTO response,
                                            ValidationContext context) {
        Map<String, Store> storesByLogoFileName = new HashMap<>();
        int created = 0;

        for (BulkStoreRowDTO row : rows) {
            String slug = storeSlug(row.getStoreName());
            if (!context.newStoreSlugs.contains(slug)) {
                continue;
            }
            Store store = storeService.createFromDTO(toStoreDTO(row));
            created++;
            if (row.getLogoFileName() != null && !row.getLogoFileName().isBlank()) {
                storesByLogoFileName.put(normalizeFileName(row.getLogoFileName()), store);
            }
        }
        response.setStoresCreated(created);
        return storesByLogoFileName;
    }

    private void processLogos(MultipartFile zipFile, BulkUploadResponseDTO response,
                              Map<String, Store> storesByLogoFileName,
                              boolean allowExistingStoreFallback) throws IOException {
        int uploaded = 0;
        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    zis.closeEntry();
                    continue;
                }

                String entryName = entry.getName();
                String ext = getExtension(entryName).toLowerCase(Locale.ROOT);

                if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
                    response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.IMAGES, 0,
                            "INVALID_EXT", BulkIncidenceDTO.IncidenceType.WARNING,
                            "Extension no permitida: " + entryName + ". Solo: jpg, jpeg, png, webp",
                            zipFile.getOriginalFilename()));
                    zis.closeEntry();
                    continue;
                }

                byte[] bytes = zis.readAllBytes();
                if (bytes.length > MAX_LOGO_SIZE_BYTES) {
                    response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.IMAGES, 0,
                            "SIZE_EXCEEDED", BulkIncidenceDTO.IncidenceType.WARNING,
                            "Imagen supera 2 MB: " + entryName, zipFile.getOriginalFilename()));
                    zis.closeEntry();
                    continue;
                }

                String fileName = baseName(entryName);
                String slug = StoreSlugUtil.toSlugBase(removeExtension(fileName));
                Store store = storesByLogoFileName.get(normalizeFileName(fileName));
                if (store == null && allowExistingStoreFallback) {
                    store = storeRepository.findBySlug(slug).orElse(null);
                }
                if (store == null) {
                    response.addIncidence(incidence(BulkIncidenceDTO.IncidenceBlock.IMAGES, 0,
                            "REF_NOT_FOUND", BulkIncidenceDTO.IncidenceType.WARNING,
                            "Logo omitido porque no corresponde a una tienda creada en esta carga: " + entryName,
                            zipFile.getOriginalFilename()));
                    zis.closeEntry();
                    continue;
                }

                String logoKey = store.getSlug() != null && !store.getSlug().isBlank()
                        ? store.getSlug()
                        : slug;
                String s3Key = "logos/" + logoKey + "." + ext;
                String contentType = "image/" + (ext.equals("jpg") ? "jpeg" : ext);
                String logoUrl = storageService.uploadBytes(s3Key, bytes, contentType);

                store.setLogoUrl(logoUrl);
                storeRepository.save(store);
                uploaded++;
                zis.closeEntry();
            }
        }
        response.setLogosUploaded(uploaded);
    }

    private List<BulkMerchantRowDTO> parseMerchantCsv(MultipartFile file) throws IOException {
        List<BulkMerchantRowDTO> result = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return result;
            }
            String[] headers = splitCsvLine(headerLine);
            Map<String, Integer> idx = buildIndex(headers);
            String line;
            int rowNum = 2;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    rowNum++;
                    continue;
                }
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
            if (headerLine == null) {
                return result;
            }
            String[] headers = splitCsvLine(headerLine);
            Map<String, Integer> idx = buildIndex(headers);
            String line;
            int rowNum = 2;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    rowNum++;
                    continue;
                }
                String[] cols = splitCsvLine(line);
                BulkStoreRowDTO row = new BulkStoreRowDTO();
                row.setRowNumber(rowNum);
                row.setStoreName(get(cols, idx, "storeName"));
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

    private CreateUserDTO toCreateUserDTO(BulkMerchantRowDTO row) {
        CreateUserDTO dto = new CreateUserDTO();
        dto.setEmail(normalizeEmail(row.getEmail()));
        dto.setPassword(row.getPassword());
        dto.setFirstName(trim(row.getFirstName()));
        dto.setPaternalSurname(trim(row.getPaternalSurname()));
        dto.setMaternalSurname(trim(row.getMaternalSurname()));
        dto.setDocumentType(DocumentType.valueOf(row.getDocumentType().trim().toUpperCase(Locale.ROOT)));
        dto.setDocumentNumber(trim(row.getDocumentNumber()));
        dto.setBirthDate(LocalDate.parse(row.getBirthDate()));
        dto.setPhone(trim(row.getPhone()));
        dto.setGender(Gender.valueOf(row.getGender().trim().toUpperCase(Locale.ROOT)));
        dto.setRuc(trim(row.getRuc()));
        dto.setRole(Role.MERCHANT);
        return dto;
    }

    private StoreDTO toStoreDTO(BulkStoreRowDTO row) {
        StoreDTO dto = new StoreDTO();
        dto.setStoreName(trim(row.getStoreName()));
        dto.setDescription(trim(row.getDescription()));
        dto.setCategoryId(Integer.parseInt(row.getCategoryId()));
        dto.setPrimaryColor(PrimaryColor.valueOf(row.getPrimaryColor().trim().toUpperCase(Locale.ROOT)));
        dto.setSecondaryColor(SecondaryColor.valueOf(row.getSecondaryColor().trim().toUpperCase(Locale.ROOT)));
        dto.setTertiaryColor(TertiaryColor.valueOf(row.getTertiaryColor().trim().toUpperCase(Locale.ROOT)));
        dto.setMerchantId(resolveMerchantId(row.getMerchantEmail())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No se pudo resolver merchantEmail para tienda: " + row.getMerchantEmail())));
        return dto;
    }

    private Optional<Integer> resolveMerchantId(String email) {
        String normalized = normalizeEmail(email);
        if (normalized == null) {
            return Optional.empty();
        }
        return userAccountRepository.findByEmail(normalized)
                .flatMap(ua -> merchantRepository.findByUserAccountId(ua.getId()))
                .map(Merchant::getId);
    }

    private boolean merchantMatches(BulkMerchantRowDTO row, UserAccount account, Merchant merchant) {
        return Boolean.TRUE.equals(account.getActive())
                && Boolean.TRUE.equals(merchant.getActive())
                && textEquals(normalizeEmail(row.getEmail()), normalizeEmail(account.getEmail()))
                && passwordHashService.matches(row.getPassword(), account.getPassword())
                && textEquals(row.getFirstName(), merchant.getFirstName())
                && textEquals(row.getPaternalSurname(), merchant.getPaternalSurname())
                && textEquals(row.getMaternalSurname(), merchant.getMaternalSurname())
                && enumTextEquals(row.getDocumentType(), merchant.getDocumentType())
                && textEquals(row.getDocumentNumber(), merchant.getDocumentNumber())
                && dateEquals(row.getBirthDate(), merchant.getBirthDate())
                && textEquals(row.getPhone(), merchant.getPhone())
                && enumTextEquals(row.getGender(), merchant.getGender())
                && textEquals(row.getRuc(), merchant.getRuc());
    }

    private boolean storeMatches(BulkStoreRowDTO row, Store store) {
        return Boolean.TRUE.equals(store.getActive())
                && store.getStoreStatus() == StoreStatus.ACTIVE
                && textEquals(StoreSlugUtil.normalizeStoreName(row.getStoreName()), store.getStoreName())
                && textEquals(row.getDescription(), store.getDescription())
                && intEquals(row.getCategoryId(), store.getCategory() != null ? store.getCategory().getId() : null)
                && enumTextEquals(row.getPrimaryColor(), store.getPrimaryColor())
                && enumTextEquals(row.getSecondaryColor(), store.getSecondaryColor())
                && enumTextEquals(row.getTertiaryColor(), store.getTertiaryColor())
                && textEquals(normalizeEmail(row.getMerchantEmail()), merchantEmail(store));
    }

    private String merchantEmail(Store store) {
        if (store.getMerchant() == null || store.getMerchant().getUserAccount() == null) {
            return null;
        }
        return normalizeEmail(store.getMerchant().getUserAccount().getEmail());
    }

    private void requireText(BulkUploadResponseDTO response, BulkIncidenceDTO.IncidenceBlock block, int row,
                             String code, String value, String message, String filename) {
        if (value == null || value.isBlank()) {
            response.addIncidence(incidence(block, row, code, BulkIncidenceDTO.IncidenceType.ERROR,
                    message, filename));
        }
    }

    private BulkIncidenceDTO incidence(BulkIncidenceDTO.IncidenceBlock block, int row,
                                       String code, BulkIncidenceDTO.IncidenceType type,
                                       String detail, String origin) {
        return BulkIncidenceDTO.builder()
                .block(block)
                .row(row)
                .code(code)
                .type(type)
                .detail(detail)
                .origin(origin)
                .build();
    }

    private <E extends Enum<E>> boolean isValidEnum(Class<E> clazz, String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            Enum.valueOf(clazz, value.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private String enumValues(Class<? extends Enum<?>> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(Enum::name)
                .toList()
                .toString();
    }

    private String invalidColorMessage(String field, String value, Class<? extends Enum<?>> enumClass) {
        return field + " invalido: " + (value == null ? "" : value)
                + ". Valores permitidos: " + enumValues(enumClass);
    }

    private Map<String, String> collectLogoEntryNames(MultipartFile zipFile) throws IOException {
        Map<String, String> names = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (!entry.isDirectory()) {
                    String baseName = baseName(entry.getName());
                    if (!baseName.isBlank()) {
                        names.put(normalizeFileName(baseName), baseName);
                    }
                }
                zis.closeEntry();
            }
        }
        return names;
    }

    private String getExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1) : "";
    }

    private String baseName(String path) {
        String normalized = path.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    private String removeExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(0, dot) : filename;
    }

    private String normalizeFileName(String filename) {
        return baseName(filename).trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        return email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean textEquals(String left, String right) {
        return Objects.equals(trim(left), trim(right));
    }

    private boolean enumTextEquals(String value, Enum<?> current) {
        return current != null && value != null
                && current.name().equals(value.trim().toUpperCase(Locale.ROOT));
    }

    private boolean dateEquals(String value, LocalDate current) {
        return current != null && value != null && current.equals(LocalDate.parse(value));
    }

    private boolean intEquals(String value, Integer current) {
        try {
            return current != null && current.equals(Integer.parseInt(value));
        } catch (Exception e) {
            return false;
        }
    }

    private String storeSlug(String storeName) {
        if (storeName == null || storeName.isBlank()) {
            return null;
        }
        return StoreSlugUtil.toSlugBase(StoreSlugUtil.normalizeStoreName(storeName));
    }

    private String[] splitCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
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
        if (i == null || i >= cols.length) {
            return null;
        }
        String val = cols[i].trim();
        return val.isEmpty() ? null : val;
    }

    private static final class ValidationContext {
        private final Set<String> seenMerchantEmails = new HashSet<>();
        private final Set<String> validMerchantEmails = new HashSet<>();
        private final Set<String> invalidMerchantEmails = new HashSet<>();
        private final Set<String> newMerchantEmails = new HashSet<>();
        private final Set<String> existingMerchantEmails = new HashSet<>();
        private final Set<String> seenStoreSlugs = new HashSet<>();
        private final Set<String> newStoreSlugs = new HashSet<>();
        private final Set<String> skippedStoreSlugs = new HashSet<>();
        private final Set<String> invalidStoreSlugs = new HashSet<>();
        private final Map<String, String> referencedLogos = new HashMap<>();
    }
}
