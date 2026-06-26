package pe.edu.pucp.kingstore.api.controller.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.kingstore.domain.dto.store.StoreDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.store.StoreService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/stores")
public class StoreController {

    private final StoreService            storeService;

    public StoreController(StoreService storeService) {
        this.storeService       = storeService;
    }

    @GetMapping
    public ResponseEntity<List<Store>> findStores(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) StoreStatus status) {
        try {
            return ResponseEntity.ok(storeService.findStores(search, status));
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(storeService.getById(id));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody StoreDTO dto) {
        try {
            return ResponseEntity.status(201).body(storeService.createFromDTO(dto));
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody StoreDTO dto) {
        try {
            return ResponseEntity.ok(storeService.updateFromDTO(id, dto));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/suspend")
    public ResponseEntity<?> suspend(@PathVariable Integer id) {
        try {
            storeService.suspend(id);
            return ResponseEntity.ok(Map.of("message","Store suspended successfully"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Integer id) {
        try {
            storeService.deactivate(id);
            return ResponseEntity.ok(Map.of("message","Store deactivated successfully"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivate(@PathVariable Integer id) {
        try {
            storeService.reactivate(id);
            return ResponseEntity.ok(Map.of("message","Store reactivated successfully"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/metrics")
    public ResponseEntity<?> getMetrics() {
        try {
            return ResponseEntity.ok(storeService.getMetrics());
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
