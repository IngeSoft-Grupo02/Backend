package pe.edu.pucp.kingstore.api.controller.public_;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.kingstore.service.store.StoreService;

import java.util.Map;

/**
 * Endpoints públicos de tiendas — no requieren autenticación.
 * Cliente 06: exploración de catálogo sin login.
 */
@RestController
@RequestMapping("/stores/public")
public class PublicStoreController {

    private final StoreService storeService;

    public PublicStoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public ResponseEntity<?> findPublicStores() {
        return ResponseEntity.ok(
                storeService.findPublicStores().stream()
                        .map(storeService::toPublicDTO)
                        .toList()
        );
    }

    @GetMapping("/{slug}")
    public ResponseEntity<?> getPublicStore(@PathVariable String slug) {
        return storeService.findPublicBySlug(slug)
                .<ResponseEntity<?>>map(store ->
                        ResponseEntity.ok(storeService.toPublicDTO(store)))
                .orElseGet(() ->
                        ResponseEntity.status(404).body(Map.of("error", "Store not found")));
    }
}