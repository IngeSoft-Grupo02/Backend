package pe.edu.pucp.kingstore.api.controller.public_;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.product.ProductService;
import pe.edu.pucp.kingstore.service.store.StoreService;

import java.util.Map;

/**
 * Endpoints públicos del catálogo de productos.
 * No requieren autenticación.
 *
 * Cliente 9: Visualización de detalle del producto.
 * Cliente 2: Descuentos por volumen visibles en el catálogo.
 */
@RestController
@RequestMapping("/stores/public/{slug}/products")
public class PublicProductController {

    private final StoreService   storeService;
    private final ProductService productService;

    public PublicProductController(StoreService storeService,
                                   ProductService productService) {
        this.storeService   = storeService;
        this.productService = productService;
    }

    // GET /stores/public/{slug}/products
    @GetMapping
    public ResponseEntity<?> findAll(@PathVariable String slug,
                                     @RequestParam(required = false) String search) {
        try {
            Store store = resolveStore(slug);
            var products = productService.findPublicByStore(store.getId());

            if (search != null && !search.isBlank()) {
                String term = search.trim().toLowerCase();
                products = products.stream()
                        .filter(p -> p.getName() != null
                                && p.getName().toLowerCase().contains(term))
                        .toList();
            }

            return ResponseEntity.ok(products.stream()
                    .map(productService::toPublicDTO)
                    .toList());

        } catch (ResourceNotFoundException | BusinessRuleException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // GET /stores/public/{slug}/products/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable String slug,
                                      @PathVariable Integer id) {
        try {
            Store store = resolveStore(slug);
            return ResponseEntity.ok(
                    productService.toPublicDTO(
                            productService.findPublicInStore(id, store.getId())));

        } catch (ResourceNotFoundException | BusinessRuleException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // ── Helper ────────────────────────────────────────────────────────────────────

    private Store resolveStore(String slug) {
        return storeService.findPublicBySlug(slug)
                .orElseThrow(() -> new BusinessRuleException("Store not found: " + slug));
    }
}