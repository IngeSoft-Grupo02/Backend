package pe.edu.pucp.kingstore.api.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.kingstore.api.context.MerchantContext;
import pe.edu.pucp.kingstore.domain.dto.product.BulkProductResultDTO;
import pe.edu.pucp.kingstore.domain.dto.product.ProductRequestDTO;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.product.ProductService;
import pe.edu.pucp.kingstore.service.storage.StorageService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
@RequestMapping("/merchant")
public class MerchantProductController extends BaseMerchantController {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_IMAGE_SIZE_BYTES = 2L * 1024 * 1024;
    private static final double DEFAULT_BULK_PRICE = 89.0;
    private static final double DEFAULT_BULK_COST = 0.0;
    private static final int MAX_PRODUCT_IMAGES = 5;

    private final ProductService  productService;
    private final StorageService  storageService;

    public MerchantProductController(MerchantContext merchantContext,
                                     ProductService productService,
                                     StorageService storageService) {
        super(merchantContext);
        this.productService = productService;
        this.storageService = storageService;
    }

    @GetMapping("/products")
    public ResponseEntity<?> products(Authentication authentication,
                                      @RequestParam(required = false) String search,
                                      @RequestParam(required = false) Boolean active,
                                      @RequestParam(required = false) Integer storeId) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            var products = productService.findByStore(store.getId());
            if (active != null) {
                products = products.stream()
                        .filter(p -> Objects.equals(p.getActive(), active))
                        .toList();
            }
            if (search != null && !search.isBlank()) {
                String term = search.trim().toLowerCase();
                products = products.stream()
                        .filter(p -> p.getName() != null
                                && p.getName().toLowerCase().contains(term))
                        .toList();
            }
            return ResponseEntity.ok(products.stream()
                    .map(productService::toResponseDTO)
                    .toList());
        });
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<?> product(Authentication authentication,
                                     @PathVariable Integer id,
                                     @RequestParam(required = false) Integer storeId) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            return ResponseEntity.ok(
                    productService.toResponseDTO(
                            productService.findInStore(id, store.getId())));
        });
    }

    @PostMapping("/products")
    public ResponseEntity<?> createProduct(Authentication authentication,
                                           @RequestParam(required = false) Integer storeId,
                                           @RequestBody ProductRequestDTO request) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            var created = productService.createForStore(store, request);
            return ResponseEntity.status(201).body(productService.toResponseDTO(created));
        });
    }

    @PutMapping("/products/{id}")
    public ResponseEntity<?> updateProduct(Authentication authentication,
                                           @PathVariable Integer id,
                                           @RequestParam(required = false) Integer storeId,
                                           @RequestBody ProductRequestDTO request) {
        return handle(() -> {
            Store store   = currentMerchantStore(authentication, storeId);
            var   product = productService.findInStore(id, store.getId());
            var   updated = productService.updateForStore(product, request);
            return ResponseEntity.ok(productService.toResponseDTO(updated));
        });
    }

    @PatchMapping("/products/{id}/active")
    public ResponseEntity<?> updateProductActive(Authentication authentication,
                                                 @PathVariable Integer id,
                                                 @RequestParam(required = false) Integer storeId,
                                                 @RequestBody ActiveRequest request) {
        return handle(() -> {
            if (request.active() == null) {
                throw new BusinessRuleException("Active flag is required");
            }
            Store store   = currentMerchantStore(authentication, storeId);
            var   product = productService.findInStore(id, store.getId());
            var   updated = productService.toggleActive(product, request.active());
            return ResponseEntity.ok(productService.toResponseDTO(updated));
        });
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<?> deleteProduct(Authentication authentication,
                                           @PathVariable Integer id,
                                           @RequestParam(required = false) Integer storeId) {
        return handle(() -> {
            Store store   = currentMerchantStore(authentication, storeId);
            var   product = productService.findInStore(id, store.getId());
            productService.delete(product.getId());
            return ResponseEntity.ok(Map.of("message", "Product deleted successfully"));
        });
    }

    @PostMapping(value = "/products/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadProductImage(Authentication authentication,
                                                @RequestParam(required = false) Integer storeId,
                                                @RequestPart("image") MultipartFile image) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            if (image == null || image.isEmpty()) {
                throw new BusinessRuleException("Product image is required");
            }
            String extension = extension(image.getOriginalFilename() == null
                    ? "" : image.getOriginalFilename());
            if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
                throw new BusinessRuleException(
                        "Invalid image extension. Allowed: jpg, jpeg, png, webp");
            }
            if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
                throw new BusinessRuleException("Product image exceeds 2 MB");
            }
            String imageUrl = productService.uploadImage(store, image, storageService);
            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
        });
    }

    @PostMapping(value = "/products/bulk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> bulkProducts(Authentication authentication,
                                          @RequestParam(required = false) Integer storeId,
                                          @RequestPart("products") MultipartFile products,
                                          @RequestPart(value = "images", required = false) MultipartFile images) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            if (products == null || products.isEmpty()) {
                throw new BusinessRuleException("El archivo CSV de productos es obligatorio");
            }

            BulkCsvData csvData = parseBulkProductsCsv(products);
            List<String> errors = new ArrayList<>(csvData.errors());
            Set<String> referencedImages = csvData.referencedImages();
            BulkZipData zipData = BulkZipData.empty();

            if (!referencedImages.isEmpty()) {
                if (images == null || images.isEmpty()) {
                    errors.add("El CSV referencia imágenes, pero no se subió un ZIP de imágenes.");
                } else {
                    zipData = collectBulkProductImages(images);
                    errors.addAll(zipData.errors());
                    for (String imageName : referencedImages) {
                        if (!zipData.images().containsKey(imageName)) {
                            errors.add("La imagen \"" + imageName + "\" no existe en el ZIP cargado.");
                        }
                    }
                }
            }

            if (!errors.isEmpty()) {
                return ResponseEntity.ok(new BulkProductResultDTO(0, 0, 0, errors));
            }

            Map<String, String> uploadedImageUrls = uploadReferencedProductImages(store, zipData.images(), referencedImages);
            int productsCreated = 0;
            int variantsProcessed = 0;

            for (BulkProductDraft productDraft : csvData.products()) {
                ProductRequestDTO request = new ProductRequestDTO();
                request.setName(productDraft.name());
                request.setDescription(productDraft.description());
                request.setPrice(productDraft.price());
                request.setCostPrice(productDraft.costPrice());
                request.setImageUrls(productDraft.imageNames().stream()
                        .map(uploadedImageUrls::get)
                        .filter(Objects::nonNull)
                        .distinct()
                        .limit(MAX_PRODUCT_IMAGES)
                        .toList());
                request.setVariants(productDraft.variants());
                request.setActive(true);

                productService.createForStore(store, request);
                productsCreated++;
                variantsProcessed += productDraft.variants().size();
            }

            return ResponseEntity.ok(new BulkProductResultDTO(
                    productsCreated,
                    variantsProcessed,
                    uploadedImageUrls.size(),
                    List.of()
            ));
        });
    }

    private BulkCsvData parseBulkProductsCsv(MultipartFile file) throws IOException {
        List<String> errors = new ArrayList<>();
        Map<String, BulkProductBuilder> products = new LinkedHashMap<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                return new BulkCsvData(List.of(), Set.of(),
                        List.of("El CSV no contiene cabeceras."));
            }

            Map<String, Integer> index = buildBulkIndex(splitCsv(headerLine));
            List<String> required = List.of("NOMBRE", "DESCRIPCION", "TALLA", "COLOR", "STOCK");
            List<String> missing = required.stream()
                    .filter(header -> !index.containsKey(header))
                    .toList();
            if (!missing.isEmpty()) {
                return new BulkCsvData(List.of(), Set.of(),
                        List.of("Faltan columnas obligatorias: " + String.join(", ", missing)));
            }

            boolean hasPriceColumn = index.containsKey("PRECIO");
            boolean hasCostColumn = index.containsKey("COSTO");
            boolean hasImagesColumn = index.containsKey("IMAGENES");
            String line;
            int rowNumber = 2;

            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    rowNumber++;
                    continue;
                }

                String[] cols = splitCsv(line);
                int errorsBefore = errors.size();
                String name = bulkValue(cols, index, "NOMBRE");
                String description = bulkValue(cols, index, "DESCRIPCION");
                String size = bulkValue(cols, index, "TALLA");
                String colorText = bulkValue(cols, index, "COLOR");
                String stockText = bulkValue(cols, index, "STOCK");
                String imageText = hasImagesColumn ? bulkValue(cols, index, "IMAGENES") : null;
                String priceText = hasPriceColumn ? bulkValue(cols, index, "PRECIO") : null;
                String costText = hasCostColumn ? bulkValue(cols, index, "COSTO") : null;

                if (name == null) errors.add("Fila " + rowNumber + ": el nombre del producto es obligatorio.");
                if (description == null) errors.add("Fila " + rowNumber + ": la descripción es obligatoria.");
                if (size == null) errors.add("Fila " + rowNumber + ": la talla es obligatoria.");
                if (colorText == null) errors.add("Fila " + rowNumber + ": el color es obligatorio.");

                Integer stock = parseInt(stockText);
                if (stock == null) {
                    errors.add("Fila " + rowNumber + ": el stock debe ser un número entero.");
                } else if (stock < 0) {
                    errors.add("Fila " + rowNumber + ": el stock no puede ser negativo.");
                }

                Color color = parseColor(colorText);
                if (colorText != null && color == null) {
                    errors.add("Fila " + rowNumber + ": color inválido. Usa Negro, Blanco, Rojo, Azul o Verde.");
                }

                Double price = hasPriceColumn ? parseDouble(priceText) : DEFAULT_BULK_PRICE;
                if (hasPriceColumn && price == null) {
                    errors.add("Fila " + rowNumber + ": el precio debe ser numérico.");
                } else if (price != null && price <= 0) {
                    errors.add("Fila " + rowNumber + ": el precio debe ser mayor a cero.");
                }

                Double costPrice = hasCostColumn && costText != null ? parseDouble(costText) : DEFAULT_BULK_COST;
                if (hasCostColumn && costText != null && costPrice == null) {
                    errors.add("Fila " + rowNumber + ": el costo debe ser numérico.");
                } else if (costPrice != null && costPrice < 0) {
                    errors.add("Fila " + rowNumber + ": el costo no puede ser negativo.");
                }
                if (price != null && costPrice != null && costPrice > price) {
                    errors.add("Fila " + rowNumber + ": el costo no puede ser mayor que el precio.");
                }

                List<String> imageNames = bulkImageNames(imageText);
                if (imageNames.size() > MAX_PRODUCT_IMAGES) {
                    errors.add("Fila " + rowNumber + ": máximo 5 imágenes por producto.");
                }
                for (String imageName : imageNames) {
                    String imageExtension = extension(imageName);
                    if (!ALLOWED_IMAGE_EXTENSIONS.contains(imageExtension)) {
                        errors.add("Fila " + rowNumber + ": imagen con formato no permitido: "
                                + imageName + ". Usa jpg, jpeg, png o webp.");
                    }
                }

                if (errors.size() > errorsBefore) {
                    rowNumber++;
                    continue;
                }

                BulkProductBuilder builder = products.computeIfAbsent(name, key ->
                        new BulkProductBuilder(name, description, price, costPrice));
                builder.validateStaticData(rowNumber, description, price, costPrice, errors);
                builder.addImages(imageNames);
                ProductRequestDTO.ProductVariantRequestDTO variant =
                        new ProductRequestDTO.ProductVariantRequestDTO();
                variant.setSize(size);
                variant.setColor(color);
                variant.setStock(stock);
                builder.addVariant(variant);
                rowNumber++;
            }
        }

        if (products.isEmpty() && errors.isEmpty()) {
            errors.add("El CSV no contiene productos para importar.");
        }

        List<BulkProductDraft> drafts = products.values().stream()
                .map(BulkProductBuilder::build)
                .toList();
        Set<String> referencedImages = new LinkedHashSet<>();
        drafts.forEach(draft -> referencedImages.addAll(draft.imageNames()));
        return new BulkCsvData(drafts, referencedImages, errors);
    }

    private BulkZipData collectBulkProductImages(MultipartFile zipFile) throws IOException {
        Map<String, BulkImageFile> images = new HashMap<>();
        List<String> errors = new ArrayList<>();

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory() || ignoredZipEntry(entry.getName())) {
                    zis.closeEntry();
                    continue;
                }

                String filename = baseName(entry.getName());
                String normalized = normalizeFileName(filename);
                String ext = extension(filename);
                if (!ALLOWED_IMAGE_EXTENSIONS.contains(ext)) {
                    errors.add("El ZIP contiene un archivo no permitido: "
                            + filename + ". Solo se aceptan jpg, jpeg, png o webp.");
                    zis.closeEntry();
                    continue;
                }

                byte[] bytes = zis.readAllBytes();
                if (bytes.length > MAX_IMAGE_SIZE_BYTES) {
                    errors.add("La imagen " + filename + " supera el máximo de 2 MB.");
                    zis.closeEntry();
                    continue;
                }
                if (images.containsKey(normalized)) {
                    errors.add("El ZIP contiene más de una imagen con el mismo nombre: " + filename + ".");
                    zis.closeEntry();
                    continue;
                }

                images.put(normalized, new BulkImageFile(filename, bytes, contentType(filename)));
                zis.closeEntry();
            }
        }

        return new BulkZipData(images, errors);
    }

    private Map<String, String> uploadReferencedProductImages(Store store,
                                                               Map<String, BulkImageFile> zipImages,
                                                               Set<String> referencedImages) {
        Map<String, String> uploaded = new HashMap<>();
        for (String imageName : referencedImages) {
            BulkImageFile image = zipImages.get(imageName);
            if (image == null) {
                continue;
            }
            String key = "products/" + storeSlug(store) + "/" + UUID.randomUUID() + "-"
                    + safeFilename(image.filename());
            uploaded.put(imageName, storageService.uploadBytes(key, image.bytes(), image.contentType()));
        }
        return uploaded;
    }

    private Map<String, Integer> buildBulkIndex(String[] headers) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i] == null ? "" : headers[i].replace("\uFEFF", "").trim().toUpperCase(Locale.ROOT);
            if (!header.isBlank()) {
                index.put(header, i);
            }
        }
        return index;
    }

    private String bulkValue(String[] cols, Map<String, Integer> index, String key) {
        Integer i = index.get(key);
        if (i == null || i >= cols.length) {
            return null;
        }
        String value = cols[i] == null ? null : cols[i].trim();
        return value == null || value.isBlank() ? null : value;
    }

    private List<String> bulkImageNames(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (String part : value.split(";")) {
            String name = normalizeFileName(part);
            if (!name.isBlank()) {
                names.add(name);
            }
        }
        return names;
    }

    private boolean ignoredZipEntry(String path) {
        String normalizedPath = path == null ? "" : path.replace('\\', '/');
        String filename = baseName(normalizedPath);
        return normalizedPath.contains("__MACOSX")
                || filename.isBlank()
                || filename.startsWith(".")
                || "Thumbs.db".equalsIgnoreCase(filename);
    }

    private String baseName(String path) {
        String normalized = path == null ? "" : path.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        return slash >= 0 ? normalized.substring(slash + 1) : normalized;
    }

    private String normalizeFileName(String filename) {
        return baseName(filename).trim().toLowerCase(Locale.ROOT);
    }

    private String safeFilename(String filename) {
        String safe = baseName(filename).replaceAll("[^a-zA-Z0-9._-]", "-");
        return safe.isBlank() ? "image" : safe;
    }

    private String storeSlug(Store store) {
        if (store.getSlug() != null && !store.getSlug().isBlank()) {
            return store.getSlug();
        }
        return "store-" + store.getId();
    }

    private record BulkCsvData(List<BulkProductDraft> products, Set<String> referencedImages, List<String> errors) {}

    private record BulkZipData(Map<String, BulkImageFile> images, List<String> errors) {
        private static BulkZipData empty() {
            return new BulkZipData(Map.of(), List.of());
        }
    }

    private record BulkImageFile(String filename, byte[] bytes, String contentType) {}

    private record BulkProductDraft(String name,
                                    String description,
                                    Double price,
                                    Double costPrice,
                                    List<String> imageNames,
                                    List<ProductRequestDTO.ProductVariantRequestDTO> variants) {}

    private static final class BulkProductBuilder {
        private final String name;
        private final String description;
        private final Double price;
        private final Double costPrice;
        private final LinkedHashSet<String> imageNames = new LinkedHashSet<>();
        private final List<ProductRequestDTO.ProductVariantRequestDTO> variants = new ArrayList<>();

        private BulkProductBuilder(String name, String description, Double price, Double costPrice) {
            this.name = name;
            this.description = description;
            this.price = price;
            this.costPrice = costPrice;
        }

        private void validateStaticData(int rowNumber, String rowDescription, Double rowPrice,
                                        Double rowCostPrice, List<String> errors) {
            if (!Objects.equals(description, rowDescription)) {
                errors.add("Fila " + rowNumber + ": el producto \"" + name
                        + "\" tiene una descripción distinta a otra fila del mismo producto.");
            }
            if (!Objects.equals(price, rowPrice)) {
                errors.add("Fila " + rowNumber + ": el producto \"" + name
                        + "\" tiene un precio distinto a otra fila del mismo producto.");
            }
            if (!Objects.equals(costPrice, rowCostPrice)) {
                errors.add("Fila " + rowNumber + ": el producto \"" + name
                        + "\" tiene un costo distinto a otra fila del mismo producto.");
            }
        }

        private void addImages(List<String> rowImages) {
            imageNames.addAll(rowImages);
        }

        private void addVariant(ProductRequestDTO.ProductVariantRequestDTO variant) {
            variants.add(variant);
        }

        private BulkProductDraft build() {
            return new BulkProductDraft(
                    name,
                    description,
                    price,
                    costPrice,
                    imageNames.stream().limit(MAX_PRODUCT_IMAGES).toList(),
                    List.copyOf(variants)
            );
        }
    }

    public record ActiveRequest(Boolean active) {}
}
