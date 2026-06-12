package pe.edu.pucp.kingstore.api.controller.merchant;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.kingstore.api.context.MerchantContext;
import pe.edu.pucp.kingstore.domain.dto.store.MerchantStoreRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.store.StoreCategoryResponse;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.storage.StorageService;
import pe.edu.pucp.kingstore.service.store.DashboardService;
import pe.edu.pucp.kingstore.service.store.StoreCategoryService;
import pe.edu.pucp.kingstore.service.store.StoreService;

import java.util.*;

@RestController
@RequestMapping("/merchant")
public class MerchantStoreController extends BaseMerchantController {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final long MAX_IMAGE_SIZE_BYTES = 2L * 1024 * 1024;

    private final StoreService          storeService;
    private final StoreCategoryService  storeCategoryService;
    private final StorageService        storageService;
    private final DashboardService dashboardService;

    public MerchantStoreController(MerchantContext merchantContext,
                                   StoreService storeService,
                                   StoreCategoryService storeCategoryService,
                                   StorageService storageService,
                                    DashboardService dashboardService) {
        super(merchantContext);
        this.storeService         = storeService;
        this.storeCategoryService = storeCategoryService;
        this.storageService       = storageService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stores")
    public ResponseEntity<?> stores(Authentication authentication) {
        return handle(() -> {
            List<Store> stores = merchantStores(authentication);
            return ResponseEntity.ok(stores.stream()
                    .map(storeService::toResponseDTO)
                    .toList());
        });
    }

    @GetMapping("/store")
    public ResponseEntity<?> currentStore(Authentication authentication,
                                          @RequestParam(required = false) Integer storeId) {
        return handle(() -> ResponseEntity.ok(
                storeService.toResponseDTO(currentMerchantStore(authentication, storeId))
        ));
    }

    @GetMapping("/categories")
    public ResponseEntity<?> categories(@RequestParam(required = false) String search) {
        return handle(() -> {
            String term = search == null ? "" : search.trim().toLowerCase();
            return ResponseEntity.ok(storeCategoryService.findActive().stream()
                    .filter(c -> term.isBlank()
                            || c.getStoreCategoryName().toLowerCase().contains(term))
                    .map(c -> new StoreCategoryResponse(c.getId(), c.getStoreCategoryName()))
                    .toList());
        });
    }

    @PostMapping(value = "/stores/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadStoreLogo(Authentication authentication,
                                             @RequestPart("logo") MultipartFile logo) {
        return handle(() -> {
            Merchant merchant = currentMerchant(authentication);
            if (logo == null || logo.isEmpty()) {
                throw new BusinessRuleException("Store logo is required");
            }
            String filename  = logo.getOriginalFilename() == null ? "" : logo.getOriginalFilename();
            String extension = extension(filename);
            if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
                throw new BusinessRuleException("Invalid logo extension. Allowed: jpg, jpeg, png, webp");
            }
            if (logo.getSize() > MAX_IMAGE_SIZE_BYTES) {
                throw new BusinessRuleException("Logo image exceeds 2 MB");
            }
            String key    = "logos/merchants/" + merchant.getId() + "/" + UUID.randomUUID() + "." + extension;
            String logoUrl = storageService.uploadBytes(key, logo.getBytes(), contentType(filename));
            return ResponseEntity.ok(Map.of("logoUrl", logoUrl));
        });
    }

    @PostMapping("/stores")
    public ResponseEntity<?> createStore(Authentication authentication,
                                         @RequestBody MerchantStoreRequestDTO request) {
        return handle(() -> {
            Merchant merchant = currentMerchant(authentication);
            Store store = storeService.createForMerchant(merchant, request);
            return ResponseEntity.status(201).body(storeService.toResponseDTO(store));
        });
    }

    @PutMapping("/stores/{id}")
    public ResponseEntity<?> updateStore(Authentication authentication,
                                         @PathVariable Integer id,
                                         @RequestBody MerchantStoreRequestDTO request) {
        return handle(() -> {
            Store store = storeInMerchantScope(authentication, id);
            Store updated = storeService.updateForMerchant(store, request);
            return ResponseEntity.ok(storeService.toResponseDTO(updated));
        });
    }

    @DeleteMapping("/stores/{id}")
    public ResponseEntity<?> deleteStore(Authentication authentication,
                                         @PathVariable Integer id) {
        return handle(() -> {
            Store store = storeInMerchantScope(authentication, id);
            storeService.deactivate(store.getId());
            return ResponseEntity.ok(Map.of("message", "Store deactivated successfully"));
        });
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> dashboard(Authentication authentication,
                                       @RequestParam(required = false) Integer storeId) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);

            return ResponseEntity.ok(dashboardService.getDashboardData(store.getId()));
        });
    }

}