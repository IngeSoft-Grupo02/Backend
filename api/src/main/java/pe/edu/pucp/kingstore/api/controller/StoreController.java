package pe.edu.pucp.kingstore.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
        import pe.edu.pucp.kingstore.domain.dto.store.StoreDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.store.StoreService;

import java.util.List;

@RestController
@RequestMapping("/admin/stores")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    // Admin-07: Listar y buscar tiendas
    @GetMapping
    public ResponseEntity<List<Store>> findStores(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) StoreStatus status){
        try {
            return ResponseEntity.ok(storeService.findStores(search, status));
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Admin-01: Registrar tienda
    @PostMapping
    public ResponseEntity<?> create(@RequestBody StoreDTO dto) {
        try {
            Store created = storeService.createFromDTO(dto);
            return ResponseEntity.status(201).body(created);
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Admin-05: Suspender tienda
    @PatchMapping("/{id}/suspend")
    public ResponseEntity<?> suspend(@PathVariable Integer id) {
        try {
            storeService.suspend(id);
            return ResponseEntity.ok("Store suspended successfully");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Admin-06: Desactivar tienda
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Integer id) {
        try {
            storeService.deactivate(id);
            return ResponseEntity.ok("Store deactivated successfully");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Reactivar tienda (parte de Admin-05)
    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivate(@PathVariable Integer id) {
        try {
            storeService.reactivate(id);
            return ResponseEntity.ok("Store reactivated successfully");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Admin-03: Métricas de tiendas
    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics() {
        try {
            return ResponseEntity.ok(storeService.getMetrics());
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}