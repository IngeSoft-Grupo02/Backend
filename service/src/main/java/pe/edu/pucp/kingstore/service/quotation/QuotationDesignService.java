package pe.edu.pucp.kingstore.service.quotation;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.QuotationDesign;
import pe.edu.pucp.kingstore.repository.quotation.QuotationDesignRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.storage.StorageService;

import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class QuotationDesignService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "application/pdf"
    );
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;

    private final QuotationDesignRepository quotationDesignRepository;
    private final StorageService storageService;

    public QuotationDesignService(QuotationDesignRepository quotationDesignRepository,
                                  StorageService storageService) {
        this.quotationDesignRepository = quotationDesignRepository;
        this.storageService = storageService;
    }

    @Transactional
    public List<QuotationDesign> uploadDesigns(Quotation quotation,
                                               String storeSlug,
                                               Integer storeId,
                                               List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        List<QuotationDesign> saved = new ArrayList<>();
        for (MultipartFile file : files.stream().filter(file -> file != null).toList()) {
            validate(file);

            String originalFileName = originalFileName(file);
            String safeFileName = safeFileName(originalFileName);
            String contentType = normalizedContentType(file);
            String key = "design/" + safePathSegment(storeSlug)
                    + "/quotations/" + quotation.getId()
                    + "/" + UUID.randomUUID() + "-" + safeFileName;
            String url = upload(key, file, contentType);

            QuotationDesign design = new QuotationDesign();
            design.setQuotation(quotation);
            design.setStoreId(storeId);
            design.setOriginalFileName(originalFileName);
            design.setStorageKey(key);
            design.setFileUrl(url);
            design.setContentType(contentType);
            design.setSizeBytes(file.getSize());
            saved.add(quotationDesignRepository.save(design));
        }

        if (!saved.isEmpty()) {
            if (quotation.getDesigns() == null) {
                quotation.setDesigns(new ArrayList<>());
            }
            quotation.getDesigns().addAll(saved);
        }
        return saved;
    }

    private void validate(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessRuleException("El archivo está vacío.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessRuleException("El archivo supera el tamaño máximo permitido.");
        }
        if (!ALLOWED_CONTENT_TYPES.contains(normalizedContentType(file))) {
            throw new BusinessRuleException("Formato de archivo no permitido.");
        }
    }

    private String upload(String key, MultipartFile file, String contentType) {
        try {
            return storageService.uploadBytes(key, file.getBytes(), contentType);
        } catch (IOException | RuntimeException ex) {
            throw new BusinessRuleException("No se pudo subir el diseño. Inténtalo nuevamente.");
        }
    }

    private String normalizedContentType(MultipartFile file) {
        return String.valueOf(file.getContentType()).split(";")[0].trim().toLowerCase(Locale.ROOT);
    }

    private String originalFileName(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name == null || name.isBlank() ? "archivo" : name.trim();
    }

    private String safePathSegment(String value) {
        String clean = safeFileName(value);
        return clean.isBlank() ? "tienda" : clean;
    }

    private String safeFileName(String value) {
        String normalized = Normalizer.normalize(String.valueOf(value), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        String clean = normalized.replaceAll("[^a-zA-Z0-9._-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^[.-]+|[.-]+$", "");
        return clean.isBlank() ? "archivo" : clean;
    }
}
