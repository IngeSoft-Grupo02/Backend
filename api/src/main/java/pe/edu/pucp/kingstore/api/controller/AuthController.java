
package pe.edu.pucp.kingstore.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.kingstore.domain.dto.user.LoginRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.user.LoginResponseDTO;
import pe.edu.pucp.kingstore.domain.model.user.enums.Role;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.security.JwtUtil;
import pe.edu.pucp.kingstore.service.store.StoreService;
import pe.edu.pucp.kingstore.service.user.UserAccountService;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserAccountService userAccountService;
    private final JwtUtil jwtUtil;
    private final StoreService storeService;

    public AuthController(UserAccountService userAccountService, JwtUtil jwtUtil, StoreService storeService) {
        this.userAccountService = userAccountService;
        this.jwtUtil = jwtUtil;
        this.storeService = storeService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequestDTO request) {
        try {
            LoginResponseDTO result = userAccountService.authenticate(request);

            // Clientes no pueden usar este endpoint
            if (result.getRole() == Role.CUSTOMER) {
                return ResponseEntity.status(403)
                        .body("Customers must login through their store endpoint");
            }
            String storeSlug = null;
            if (result.getRole() == Role.MERCHANT){
                storeSlug = storeService.findActiveSlugByUserAccountId(result.getId())
                        .orElseThrow(()-> new BusinessRuleException("Merchant has no active store assigned"));
                result.setStoreSlug(storeSlug);
            }
            String token = jwtUtil.generateToken(
                    result.getId(),
                    result.getEmail(),
                    result.getRole(),
                    storeSlug
            );

            result.setToken(token);
            return ResponseEntity.ok(result);

        } catch (BusinessRuleException e) {
            return ResponseEntity.status(401).body(e.getMessage());
        }
    }
}