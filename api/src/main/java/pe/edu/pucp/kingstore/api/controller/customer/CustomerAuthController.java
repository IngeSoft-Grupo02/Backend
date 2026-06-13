package pe.edu.pucp.kingstore.api.controller.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.kingstore.domain.dto.user.LoginRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.user.LoginResponseDTO;
import pe.edu.pucp.kingstore.domain.dto.user.RegisterCustomerDTO;
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
                                      @RequestBody RegisterCustomerDTO dto) {
        try {
            userAccountService.registerCustomer(dto, slug);
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
            LoginResponseDTO result = userAccountService.authenticateCustomer(slug, request);
            String token = jwtUtil.generateToken(
                    result.getId(),
                    result.getEmail(),
                    Role.CUSTOMER,
                    result.getStoreSlug()
            );
            result.setToken(token);
            return ResponseEntity.ok(result);
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }

    // Cliente-06: Perfil del cliente autenticado
    @GetMapping("/customers/me")
    public ResponseEntity<?> me(@PathVariable String slug,
                                Authentication authentication) {
        try {
            Integer userAccountId = resolveUserAccountId(authentication);
            return ResponseEntity.ok(
                    userAccountService.getCustomerProfile(userAccountId, slug));
        } catch (BusinessRuleException e) {
            return ResponseEntity.status(403).body(e.getMessage());
        }
    }

    private Integer resolveUserAccountId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessRuleException("Authenticated customer is required");
        }
        try {
            return Integer.parseInt(authentication.getName());
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("Authenticated customer id is invalid");
        }
    }
}