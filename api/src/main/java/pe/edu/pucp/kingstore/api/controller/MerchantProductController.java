package pe.edu.pucp.kingstore.api.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.kingstore.api.context.MerchantContext;
import pe.edu.pucp.kingstore.domain.dto.product.ProductRequestDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.product.ProductService;
import pe.edu.pucp.kingstore.service.storage.StorageService;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/merchant")
public class MerchantProductController extends BaseMerchantController {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS =
            Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_IMAGE_SIZE_BYTES = 2L * 1024 * 1024;

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

    public record ActiveRequest(Boolean active) {}
}