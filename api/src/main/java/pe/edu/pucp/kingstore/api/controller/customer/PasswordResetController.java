package pe.edu.pucp.kingstore.api.controller.customer;

import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.kingstore.domain.dto.user.PasswordResetConfirmDTO;
import pe.edu.pucp.kingstore.domain.dto.user.PasswordResetRequestDTO;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.user.PasswordResetService;

import java.util.Map;

@RestController
@RequestMapping("/auth/password")
public class PasswordResetController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetController.class);
    private static final String GENERIC_MESSAGE =
            "Si el correo corresponde a una cuenta activa, recibirás un enlace de recuperación.";

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot")
    public ResponseEntity<Map<String, String>> forgotPassword(@RequestBody PasswordResetRequestDTO request) {
        try {
            passwordResetService.requestReset(
                    request != null ? request.getEmail() : null,
                    request != null ? request.getStoreSlug() : null
            );
        } catch (BusinessRuleException exception) {
            if ("MERCHANT_WITHOUT_STORE".equals(exception.getMessage())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "code", "MERCHANT_WITHOUT_STORE",
                        "message", "Tu cuenta de comerciante aún no tiene una tienda asignada. No puedes recuperar la contraseña hasta que se asocie una tienda."
                ));
            }
            if ("CUSTOMER_NOT_REGISTERED_IN_STORE".equals(exception.getMessage())) {
                return ResponseEntity.badRequest().body(Map.of(
                        "code", "CUSTOMER_NOT_REGISTERED_IN_STORE",
                        "message", "Este correo no pertenece a ningún cliente registrado en esta tienda. Regístrate para crear una cuenta."
                ));
            }
            LOGGER.error("Password reset request could not be processed", exception);
        } catch (RuntimeException exception) {
            LOGGER.error("Password reset email could not be sent", exception);
        }
        return ResponseEntity.ok(Map.of("message", GENERIC_MESSAGE));
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Boolean>> validateToken(@RequestParam String token) {
        return ResponseEntity.ok(Map.of("valid", passwordResetService.isTokenValid(token)));
    }

    @PostMapping("/reset")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetConfirmDTO request) {
        try {
            passwordResetService.resetPassword(request);
            return ResponseEntity.ok(Map.of("message", "Contraseña actualizada correctamente."));
        } catch (BusinessRuleException exception) {
            String code = exception.getMessage();
            String message = "PASSWORD_POLICY_INVALID".equals(code)
                    ? "La contraseña debe tener entre 8 y 72 caracteres e incluir mayúscula, minúscula, número y símbolo."
                    : "El enlace es inválido, expiró o ya fue utilizado.";
            return ResponseEntity.badRequest().body(Map.of("code", code, "message", message));
        }
    }
}
