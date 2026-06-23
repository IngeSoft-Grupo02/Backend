package pe.edu.pucp.kingstore.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.kingstore.api.context.MerchantContext;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.product.CustomDesignService;

import java.util.Map;

/**
 * Endpoints para que el merchant gestione diseños personalizados.
 * Cliente 12: Revisar y aprobar/rechazar diseños enviados por clientes.
 */
@RestController
@RequestMapping("/merchant")
public class MerchantCustomDesignController extends BaseMerchantController {

    private final CustomDesignService customDesignService;

    public MerchantCustomDesignController(MerchantContext merchantContext,
                                          CustomDesignService customDesignService) {
        super(merchantContext);
        this.customDesignService = customDesignService;
    }

    // GET /merchant/designs
    @GetMapping("/designs")
    public ResponseEntity<?> findAll(Authentication authentication,
                                     @RequestParam(required = false) Integer storeId) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            return ResponseEntity.ok(customDesignService.findByStore(store.getId()));
        });
    }

    // PATCH /merchant/designs/{id}/approve
    @PatchMapping("/designs/{id}/approve")
    public ResponseEntity<?> approve(Authentication authentication,
                                     @PathVariable Integer id,
                                     @RequestParam(required = false) Integer storeId) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            return ResponseEntity.ok(customDesignService.approve(id, store.getId()));
        });
    }

    // PATCH /merchant/designs/{id}/reject
    @PatchMapping("/designs/{id}/reject")
    public ResponseEntity<?> reject(Authentication authentication,
                                    @PathVariable Integer id,
                                    @RequestParam(required = false) Integer storeId,
                                    @RequestBody Map<String, String> body) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            String observations = body.get("observations");
            if (observations == null || observations.isBlank()) {
                throw new BusinessRuleException("Observations are required when rejecting a design");
            }
            return ResponseEntity.ok(
                    customDesignService.reject(id, store.getId(), observations));
        });
    }
}