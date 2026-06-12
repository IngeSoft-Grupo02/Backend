package pe.edu.pucp.kingstore.api.controller.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
        import pe.edu.pucp.kingstore.domain.dto.user.CreateUserDTO;
import pe.edu.pucp.kingstore.domain.dto.user.LoginRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.user.LoginResponseDTO;
import pe.edu.pucp.kingstore.domain.model.user.enums.Role;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.security.JwtUtil;
import pe.edu.pucp.kingstore.service.user.UserAccountService;

@RestController
@RequestMapping("/stores/{slug}")
public class CustomerAuthController {

    private final UserAccountService userAccountService;
    private final JwtUtil jwtUtil;

    public CustomerAuthController(UserAccountService userAccountService,
                                  JwtUtil jwtUtil) {
        this.userAccountService = userAccountService;
        this.jwtUtil = jwtUtil;
    }

    // Cliente-04: Registro en tienda
    @PostMapping("/customers/register")
    public ResponseEntity<?> register(@PathVariable String slug,
                                      @RequestBody CreateUserDTO dto) {
        try {
            // Verificar que la tienda existe y estÃ¡ activa
            dto.setRole(Role.CUSTOMER);
            userAccountService.createWithRole(dto,slug);
            return ResponseEntity.status(201).body("Customer registered successfully");
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // Cliente-05: Login en tienda
    @PostMapping("/auth/login")
    public ResponseEntity<?> login(@PathVariable String slug,
                                   @RequestBody LoginRequestDTO request) {
        try {
            // Verificar que la tienda existe y estÃ¡ activa
            LoginResponseDTO result = userAccountService.authenticateCustomer(slug, request);
            String token = jwtUtil.generateToken(
                    result.getId(),
                    result.getEmail(),
                    Role.CUSTOMER,
                    slug
            );

            result.setToken(token);
            return ResponseEntity.ok(result);
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}