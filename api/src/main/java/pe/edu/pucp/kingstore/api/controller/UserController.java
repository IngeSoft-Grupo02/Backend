    package pe.edu.pucp.kingstore.api.controller;

    import org.springframework.http.ResponseEntity;
    import org.springframework.web.bind.annotation.*;
            import pe.edu.pucp.kingstore.domain.dto.user.CreateUserDTO;
    import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
    import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
    import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
    import pe.edu.pucp.kingstore.service.user.UserAccountService;

    @RestController
    @RequestMapping("/admin/users")
    public class UserController {

        private final UserAccountService userAccountService;

        public UserController(UserAccountService userAccountService) {
            this.userAccountService = userAccountService;
        }

        // Admin-02: Crear usuario con rol
        @PostMapping
        public ResponseEntity<?> create(@RequestBody CreateUserDTO dto) {
            try {
                UserAccount created = userAccountService.createWithRole(dto);
                return ResponseEntity.status(201).body(created);
            } catch (BusinessRuleException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }

        // Admin-02: Obtener usuario por id
        @GetMapping("/{id}")
        public ResponseEntity<?> getById(@PathVariable Integer id) {
            try {
                UserAccount account = userAccountService.getById(id);
                return ResponseEntity.ok(account);
            } catch (ResourceNotFoundException e) {
                return ResponseEntity.status(404).body(e.getMessage());
            }
        }
        // Admin-02: Modificar usuario
        @PutMapping("/{id}")
        public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody CreateUserDTO dto) {
            try {
                UserAccount updated = userAccountService.updateUser(id, dto);
                return ResponseEntity.ok(updated);
            } catch (ResourceNotFoundException e) {
                return ResponseEntity.status(404).body(e.getMessage());
            } catch (BusinessRuleException e) {
                return ResponseEntity.badRequest().body(e.getMessage());
            }
        }

        // Admin-02: Desactivar usuario
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

        // Admin-02: Reactivar usuario
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


    }