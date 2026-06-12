package pe.edu.pucp.kingstore.api.controller.merchant;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.kingstore.api.context.MerchantContext;
import pe.edu.pucp.kingstore.domain.dto.product.DiscountRequestDTO;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.service.product.DiscountService;
import pe.edu.pucp.kingstore.service.product.ProductService;

@RestController
@RequestMapping("/merchant")
public class MerchantDiscountController extends BaseMerchantController {

    private final DiscountService discountService;
    private final ProductService  productService;

    public MerchantDiscountController(MerchantContext merchantContext,
                                      DiscountService discountService,
                                      ProductService productService) {
        super(merchantContext);
        this.discountService = discountService;
        this.productService  = productService;
    }

    @GetMapping("/discounts")
    public ResponseEntity<?> discounts(Authentication authentication,
                                       @RequestParam(required = false) Integer storeId) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            return ResponseEntity.ok(
                    discountService.findByStoreId(store.getId()).stream()
                            .map(discountService::toResponseDTO)
                            .toList());
        });
    }

    @PostMapping("/discounts")
    public ResponseEntity<?> createDiscount(Authentication authentication,
                                            @RequestParam(required = false) Integer storeId,
                                            @RequestBody DiscountRequestDTO request) {
        return handle(() -> {
            Store   store   = currentMerchantStore(authentication, storeId);
            Product product = request.getProductId() == null ? null
                    : productService.findInStore(request.getProductId(), store.getId());
            var created = discountService.createForStore(store, product, request);
            return ResponseEntity.status(201).body(discountService.toResponseDTO(created));
        });
    }

    @PutMapping("/discounts/{id}")
    public ResponseEntity<?> updateDiscount(Authentication authentication,
                                            @PathVariable Integer id,
                                            @RequestParam(required = false) Integer storeId,
                                            @RequestBody DiscountRequestDTO request) {
        return handle(() -> {
            Store    store    = currentMerchantStore(authentication, storeId);
            var      discount = discountService.findInStore(id, store.getId());
            Product  product  = request.getProductId() == null ? null
                    : productService.findInStore(request.getProductId(), store.getId());
            var updated = discountService.updateForStore(discount, store, product, request);
            return ResponseEntity.ok(discountService.toResponseDTO(updated));
        });
    }

    @DeleteMapping("/discounts/{id}")
    public ResponseEntity<?> deleteDiscount(Authentication authentication,
                                            @PathVariable Integer id,
                                            @RequestParam(required = false) Integer storeId) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            discountService.findInStore(id, store.getId()); // scope guard
            discountService.deactivate(id);
            return ResponseEntity.ok(java.util.Map.of("message", "Discount deactivated successfully"));
        });
    }
}