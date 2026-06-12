package pe.edu.pucp.kingstore.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.kingstore.domain.dto.store.StorePublicDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.service.store.StoreService;

import java.util.List;
import java.util.Map;

// Cliente-01/02: Catálogo público de tiendas
@RestController
@RequestMapping("/stores/public")
public class PublicStoreController {

    private final StoreService storeService;

    public PublicStoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping
    public ResponseEntity<List<StorePublicDTO>> findPublicStores() {
        List<StorePublicDTO> stores = storeService.findPublicStores().stream()
                .map(PublicStoreController::toPublicDTO)
                .toList();
        return ResponseEntity.ok(stores);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<?> getPublicStore(@PathVariable String slug) {
        return storeService.findPublicBySlug(slug)
                .<ResponseEntity<?>>map(store -> ResponseEntity.ok(toPublicDTO(store)))
                .orElseGet(() -> ResponseEntity.status(404).body(Map.of("error", "Store not found")));
    }

    private static StorePublicDTO toPublicDTO(Store store) {
        StorePublicDTO dto = new StorePublicDTO();
        dto.setId(store.getId());
        dto.setStoreName(store.getStoreName());
        dto.setSlug(store.getSlug());
        dto.setDescription(store.getDescription());
        dto.setLogoUrl(store.getLogoUrl());
        dto.setCategory(store.getCategory() != null ? store.getCategory().getStoreCategoryName() : null);
        dto.setPrimaryColor(store.getPrimaryColor());
        dto.setSecondaryColor(store.getSecondaryColor());
        dto.setTertiaryColor(store.getTertiaryColor());
        return dto;
    }
}
