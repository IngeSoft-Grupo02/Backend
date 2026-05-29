package pe.edu.pucp.kingstore.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.kingstore.domain.dto.store.StoreDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.repository.store.StoreCategoryRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.store.StoreService;

import java.util.List;

@RestController
@RequestMapping("/admin/stores")
public class StoreController {

    private final StoreService            storeService;
    private final MerchantRepository      merchantRepository;
    private final StoreCategoryRepository categoryRepository;

    public StoreController(StoreService storeService,
                           MerchantRepository merchantRepository,
                           StoreCategoryRepository categoryRepository) {
        this.storeService       = storeService;
        this.merchantRepository = merchantRepository;
        this.categoryRepository = categoryRepository;
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
            Store store = storeService.getById(id);
            if (dto.getStoreName()     != null) store.setStoreName(dto.getStoreName());
            if (dto.getSlug()          != null) store.setSlug(dto.getSlug());
            if (dto.getDescription()   != null) store.setDescription(dto.getDescription());
            if (dto.getPrimaryColor()  != null) store.setPrimaryColor(dto.getPrimaryColor());
            if (dto.getSecondaryColor()!= null) store.setSecondaryColor(dto.getSecondaryColor());
            if (dto.getTertiaryColor() != null) store.setTertiaryColor(dto.getTertiaryColor());
            if (dto.getMerchantId()    != null)
                merchantRepository.findById(dto.getMerchantId()).ifPresent(store::setMerchant);
            if (dto.getCategoryId()    != null)
                categoryRepository.findById(dto.getCategoryId()).ifPresent(store::setCategory);
            return ResponseEntity.ok(storeService.update(id, store));
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
            return ResponseEntity.ok("Store suspended successfully");
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
            return ResponseEntity.ok("Store deactivated successfully");
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
            return ResponseEntity.ok("Store reactivated successfully");
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
