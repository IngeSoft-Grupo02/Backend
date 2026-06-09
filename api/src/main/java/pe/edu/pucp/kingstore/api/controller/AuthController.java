
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

import java.util.Map;

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
                        .body(authError("ROLE_NOT_ALLOWED", "Esta cuenta no pertenece al panel de comerciantes."));
            }
            String storeSlug = null;
            if (result.getRole() == Role.MERCHANT){
                storeSlug = storeService.findLoginSlugByUserAccountId(result.getId())
                        .orElseThrow(()-> new BusinessRuleException("MERCHANT_NO_STORE"));
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
            return ResponseEntity.status(authStatus(e.getMessage())).body(authError(e.getMessage(), authMessage(e.getMessage())));
        }
    }

    private int authStatus(String code) {
        return switch (code) {
            case "ACCOUNT_INACTIVE", "ROLE_NOT_ALLOWED", "ROLE_NOT_ASSIGNED", "MERCHANT_NO_STORE" -> 403;
            default -> 401;
        };
    }

    private String authMessage(String code) {
        return switch (code) {
            case "ACCOUNT_NOT_FOUND" -> "No existe una cuenta registrada con ese correo.";
            case "ACCOUNT_INACTIVE" -> "Esta cuenta estÃƒÂ¡ inactiva. Contacta al administrador.";
            case "BAD_CREDENTIALS" -> "La contraseÃƒÂ±a ingresada es incorrecta.";
            case "ROLE_NOT_ALLOWED" -> "Esta cuenta no pertenece al panel de comerciantes.";
            case "ROLE_NOT_ASSIGNED" -> "Esta cuenta no tiene un rol asignado.";
            case "MERCHANT_NO_STORE" -> "Tu cuenta de comerciante no tiene una tienda asignada.";
            case "Email is required" -> "Ingresa tu correo electrÃƒÂ³nico.";
            case "Password is required" -> "Ingresa tu contraseÃƒÂ±a.";
            default -> "No se pudo iniciar sesiÃƒÂ³n. Revisa tus credenciales.";
        };
    }

    private Map<String, String> authError(String code, String message) {
        return Map.of("code", code, "message", message);
    }
}

