package pe.edu.pucp.kingstore.api.controller.admin;

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
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.CustomerRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.repository.user.SystemAdministratorRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.user.UserAccountService;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/users")
public class UserController {

    private final UserAccountService            userAccountService;
    private final UserAccountRepository         userAccountRepository;
    private final MerchantRepository            merchantRepository;
    private final CustomerRepository            customerRepository;
    private final SystemAdministratorRepository adminRepository;
    private final StoreRepository               storeRepository;

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

    // â”€â”€ GET /admin/users â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Carga todos los datos en memoria de una sola vez (bulk load)
    // evitando N+1 queries por usuario
    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> findAll(
            @RequestParam(required = false) String search) {

        // 1. Cargar todos en paralelo â€” 4 queries totales, no N*4
        List<UserAccount>         accounts  = userAccountRepository.findAll();
        List<Merchant>            merchants = merchantRepository.findAll();
        List<Customer>            customers = customerRepository.findAll();
        List<SystemAdministrator> admins    = adminRepository.findAll();
        List<Store>               stores    = storeRepository.findAll();

        // 2. Construir mapas para lookup O(1)
        Map<Integer, Merchant>            merchantByUaId = merchants.stream()
                .collect(Collectors.toMap(m -> m.getUserAccount().getId(), m -> m, (a, b) -> a));
        Map<Integer, Customer>            customerByUaId = customers.stream()
                .collect(Collectors.toMap(c -> c.getUserAccount().getId(), c -> c, (a, b) -> a));
        Map<Integer, SystemAdministrator> adminByUaId    = admins.stream()
                .collect(Collectors.toMap(a -> a.getUserAccount().getId(), a -> a, (a, b) -> a));
        // Tienda por merchantId
        Map<Integer, Store> storeByMerchantId = stores.stream()
                .filter(s -> s.getMerchant() != null)
                .collect(Collectors.toMap(s -> s.getMerchant().getId(), s -> s, (a, b) -> a));
        // Tienda por customerId (person_id)
        Map<Integer, Store> storeByCustomerId = customers.stream()
                .filter(c -> c.getStore() != null)
                .collect(Collectors.toMap(c -> c.getId(), c -> c.getStore(), (a, b) -> a));

        // 3. Mapear
        List<UserResponseDTO> result = new ArrayList<>();
        for (UserAccount ua : accounts) {
            UserResponseDTO dto = buildFromMaps(ua,
                    merchantByUaId, customerByUaId, adminByUaId,
                    storeByMerchantId, storeByCustomerId);
            result.add(dto);
        }

        // 4. Filtrar si hay search
        if (search != null && !search.isBlank()) {
            String term = search.toLowerCase();
            result = result.stream()
                    .filter(d -> d.getEmail().toLowerCase().contains(term)
                            || (d.getFirstName()       != null && d.getFirstName().toLowerCase().contains(term))
                            || (d.getPaternalSurname() != null && d.getPaternalSurname().toLowerCase().contains(term)))
                    .toList();
        }

        return ResponseEntity.ok(result);
    }

    // â”€â”€ GET /admin/users/{id} â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Integer id) {
        try {
            UserAccount account = userAccountService.getById(id);
            // Cargar los datos del usuario especÃ­fico
            List<Merchant>            merchants = merchantRepository.findAll();
            List<Customer>            customers = customerRepository.findAll();
            List<SystemAdministrator> admins    = adminRepository.findAll();
            List<Store>               stores    = storeRepository.findAll();

            Map<Integer, Merchant>            mByUa = merchants.stream()
                    .collect(Collectors.toMap(m -> m.getUserAccount().getId(), m -> m, (a, b) -> a));
            Map<Integer, Customer>            cByUa = customers.stream()
                    .collect(Collectors.toMap(c -> c.getUserAccount().getId(), c -> c, (a, b) -> a));
            Map<Integer, SystemAdministrator> aByUa = admins.stream()
                    .collect(Collectors.toMap(a -> a.getUserAccount().getId(), a -> a, (a, b) -> a));
            Map<Integer, Store> storeByMerchantId = stores.stream()
                    .filter(s -> s.getMerchant() != null)
                    .collect(Collectors.toMap(s -> s.getMerchant().getId(), s -> s, (a, b) -> a));
            Map<Integer, Store> storeByCustomerId = customers.stream()
                    .filter(c -> c.getStore() != null)
                    .collect(Collectors.toMap(c -> c.getId(), c -> c.getStore(), (a, b) -> a));

            return ResponseEntity.ok(buildFromMaps(account, mByUa, cByUa, aByUa, storeByMerchantId, storeByCustomerId));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // â”€â”€ POST /admin/users â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @PostMapping
    public ResponseEntity<?> create(@RequestBody CreateUserDTO dto) {
        try {
            UserAccount created = userAccountService.createWithRole(dto);
            return ResponseEntity.status(201).body(Map.of("id", created.getId(), "email", created.getEmail()));
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Error al crear el usuario: " + e.getMessage()));
        }
    }

    // â”€â”€ PUT /admin/users/{id} â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody CreateUserDTO dto) {
        try {
            UserAccount updated = userAccountService.updateUser(id, dto);
            return ResponseEntity.ok(Map.of("id", updated.getId(), "email", updated.getEmail()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // â”€â”€ PATCH deactivate/reactivate â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<?> deactivate(@PathVariable Integer id) {
        try {
            userAccountService.deactivate(id);
            return ResponseEntity.ok(Map.of("message", "User deactivated successfully"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<?> reactivate(@PathVariable Integer id) {
        try {
            userAccountService.reactivate(id);
            return ResponseEntity.ok(Map.of("message", "User reactivated successfully"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // â”€â”€ GET /admin/users/merchants â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
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

    // â”€â”€ Helper: construir DTO desde mapas pre-cargados â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private UserResponseDTO buildFromMaps(
            UserAccount ua,
            Map<Integer, Merchant>            merchantByUaId,
            Map<Integer, Customer>            customerByUaId,
            Map<Integer, SystemAdministrator> adminByUaId,
            Map<Integer, Store>               storeByMerchantId,
            Map<Integer, Store>               storeByCustomerId) {

        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(ua.getId());
        dto.setEmail(ua.getEmail());
        dto.setActive(ua.getActive());

        Merchant merchant = merchantByUaId.get(ua.getId());
        if (merchant != null) {
            dto.setRole("MERCHANT");
            dto.setFirstName(merchant.getFirstName());
            dto.setPaternalSurname(merchant.getPaternalSurname());
            dto.setMaternalSurname(merchant.getMaternalSurname());
            dto.setDocumentNumber(merchant.getDocumentNumber());
            dto.setDocumentType(merchant.getDocumentType() != null ? merchant.getDocumentType().name() : null);
            dto.setBirthDate(merchant.getBirthDate());
            dto.setPhone(merchant.getPhone());
            dto.setGender(merchant.getGender() != null ? merchant.getGender().name() : null);
            dto.setRuc(merchant.getRuc());
            Store store = storeByMerchantId.get(merchant.getId());
            if (store != null) { dto.setStoreName(store.getStoreName()); dto.setStoreId(store.getId()); }
            return dto;
        }

        Customer customer = customerByUaId.get(ua.getId());
        if (customer != null) {
            dto.setRole("CUSTOMER");
            dto.setFirstName(customer.getFirstName());
            dto.setPaternalSurname(customer.getPaternalSurname());
            dto.setMaternalSurname(customer.getMaternalSurname());
            dto.setDocumentNumber(customer.getDocumentNumber());
            dto.setDocumentType(customer.getDocumentType() != null ? customer.getDocumentType().name() : null);
            dto.setBirthDate(customer.getBirthDate());
            dto.setPhone(customer.getPhone());
            dto.setGender(customer.getGender() != null ? customer.getGender().name() : null);
            Store store = storeByCustomerId.get(customer.getId());
            if (store != null) { dto.setStoreName(store.getStoreName()); dto.setStoreId(store.getId()); }
            return dto;
        }

        SystemAdministrator admin = adminByUaId.get(ua.getId());
        if (admin != null) {
            dto.setRole("SYSTEM_ADMIN");
            dto.setFirstName(admin.getFirstName());
            dto.setPaternalSurname(admin.getPaternalSurname());
            dto.setMaternalSurname(admin.getMaternalSurname());
            dto.setPhone(admin.getPhone());
            dto.setDocumentNumber(admin.getDocumentNumber());
            dto.setDocumentType(admin.getDocumentType() != null ? admin.getDocumentType().name() : null);
            dto.setBirthDate(admin.getBirthDate());
            dto.setGender(admin.getGender() != null ? admin.getGender().name() : null);
            return dto;
        }

        dto.setRole("UNKNOWN");
        return dto;
    }
}
