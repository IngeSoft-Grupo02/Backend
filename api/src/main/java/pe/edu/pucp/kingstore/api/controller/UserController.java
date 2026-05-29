package pe.edu.pucp.kingstore.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.kingstore.domain.dto.user.CreateUserDTO;
import pe.edu.pucp.kingstore.domain.dto.user.MerchantResponseDTO;
import pe.edu.pucp.kingstore.domain.dto.user.UserResponseDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.domain.model.user.SystemAdministrator;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
import pe.edu.pucp.kingstore.domain.model.user.enums.Role;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.CustomerRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.repository.user.SystemAdministratorRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.user.UserAccountService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/admin/users")
public class UserController {

    private final UserAccountService         userAccountService;
    private final UserAccountRepository      userAccountRepository;
    private final MerchantRepository         merchantRepository;
    private final CustomerRepository         customerRepository;
    private final SystemAdministratorRepository adminRepository;
    private final StoreRepository            storeRepository;

    public UserController(
            UserAccountService userAccountService,
            UserAccountRepository userAccountRepository,
            MerchantRepository merchantRepository,
            CustomerRepository customerRepository,
            SystemAdministratorRepository adminRepository,
            StoreRepository storeRepository) {
        this.userAccountService    = userAccountService;
        this.userAccountRepository = userAccountRepository;
        this.merchantRepository    = merchantRepository;
        this.customerRepository    = customerRepository;
        this.adminRepository       = adminRepository;
        this.storeRepository       = storeRepository;
    }

    // ── GET /admin/users  →  listado completo con rol y tienda ────

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> findAll(
            @RequestParam(required = false) String search) {

        List<UserAccount> accounts = userAccountRepository.findAll();

        if (search != null && !search.isBlank()) {
            String term = search.toLowerCase();
            accounts = accounts.stream()
                    .filter(u -> u.getEmail().toLowerCase().contains(term))
                    .toList();
        }

        List<UserResponseDTO> result = new ArrayList<>();
        for (UserAccount ua : accounts) {
            result.add(buildUserResponse(ua));
        }
        return ResponseEntity.ok(result);
    }

    // ── GET /admin/users/{id}  →  usuario por id ──────────────────

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        try {
            UserAccount account = userAccountService.getById(id);
            return ResponseEntity.ok(buildUserResponse(account));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        }
    }

    // ── POST /admin/users  →  crear usuario con rol ───────────────

    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateUserDTO dto) {
        try {
            UserAccount created = userAccountService.createWithRole(dto);
            return ResponseEntity.status(201).body(buildUserResponse(created));
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── PUT /admin/users/{id}  →  actualizar usuario ──────────────

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody CreateUserDTO dto) {
        try {
            UserAccount updated = userAccountService.updateUser(id, dto);
            return ResponseEntity.ok(buildUserResponse(updated));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── PATCH /admin/users/{id}/deactivate ────────────────────────

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Integer id) {
        try {
            userAccountService.deactivate(id);
            return ResponseEntity.ok("User deactivated successfully");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── PATCH /admin/users/{id}/reactivate ────────────────────────

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivate(@PathVariable Integer id) {
        try {
            userAccountService.reactivate(id);
            return ResponseEntity.ok("User reactivated successfully");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(e.getMessage());
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ── GET /admin/merchants  →  lista para selector de comerciantes

    @GetMapping("/merchants")
    public ResponseEntity<List<MerchantResponseDTO>> findMerchants(
            @RequestParam(required = false) String search) {

        List<Merchant> merchants = merchantRepository.findAll();

        if (search != null && !search.isBlank()) {
            String term = search.toLowerCase();
            merchants = merchants.stream()
                    .filter(m -> m.getUserAccount().getEmail().toLowerCase().contains(term)
                            || m.getFirstName().toLowerCase().contains(term)
                            || m.getPaternalSurname().toLowerCase().contains(term))
                    .toList();
        }

        List<MerchantResponseDTO> result = merchants.stream().map(m -> {
            MerchantResponseDTO dto = new MerchantResponseDTO();
            dto.setId(m.getId());
            dto.setEmail(m.getUserAccount().getEmail());
            dto.setFirstName(m.getFirstName());
            dto.setPaternalSurname(m.getPaternalSurname());
            dto.setRuc(m.getRuc());
            return dto;
        }).toList();

        return ResponseEntity.ok(result);
    }

    // ── Helper: construir UserResponseDTO ────────────────────────

    private UserResponseDTO buildUserResponse(UserAccount ua) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(ua.getId());
        dto.setEmail(ua.getEmail());
        dto.setActive(ua.getActive());

        // Merchant
        Optional<Merchant> merchant = merchantRepository.findByUserAccountId(ua.getId());
        if (merchant.isPresent()) {
            Merchant m = merchant.get();
            dto.setRole("MERCHANT");
            dto.setFirstName(m.getFirstName());
            dto.setPaternalSurname(m.getPaternalSurname());
            dto.setMaternalSurname(m.getMaternalSurname());
            dto.setDocumentNumber(m.getDocumentNumber());
            dto.setDocumentType(m.getDocumentType() != null ? m.getDocumentType().name() : null);
            dto.setPhone(m.getPhone());
            dto.setRuc(m.getRuc());
            // Buscar tienda del comerciante
            storeRepository.findAll().stream()
                    .filter(s -> s.getMerchant() != null && s.getMerchant().getId().equals(m.getId()))
                    .findFirst()
                    .ifPresent(s -> { dto.setStoreName(s.getStoreName()); dto.setStoreId(s.getId()); });
            return dto;
        }

        // Customer
        Optional<Customer> customer = customerRepository.findByUserAccountId(ua.getId());
        if (customer.isPresent()) {
            Customer c = customer.get();
            dto.setRole("CUSTOMER");
            dto.setFirstName(c.getFirstName());
            dto.setPaternalSurname(c.getPaternalSurname());
            dto.setMaternalSurname(c.getMaternalSurname());
            dto.setDocumentNumber(c.getDocumentNumber());
            dto.setPhone(c.getPhone());
            // Buscar tienda del cliente
            if (c.getStore() != null) {
                dto.setStoreName(c.getStore().getStoreName());
                dto.setStoreId(c.getStore().getId());
            }
            return dto;
        }

        // Admin
        adminRepository.findByUserAccountId(ua.getId()).ifPresent(a -> {
            dto.setRole("SYSTEM_ADMIN");
            dto.setFirstName(a.getFirstName());
            dto.setPaternalSurname(a.getPaternalSurname());
        });

        if (dto.getRole() == null) dto.setRole("UNKNOWN");
        return dto;
    }
}
