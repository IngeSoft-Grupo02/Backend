package pe.edu.pucp.kingstore.api.controller.customer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pe.edu.pucp.kingstore.domain.dto.user.PasswordResetConfirmDTO;
import pe.edu.pucp.kingstore.domain.dto.user.PasswordResetRequestDTO;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.user.PasswordResetService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetControllerTest {

    @Mock
    private PasswordResetService passwordResetService;

    @Test
    void forgotPasswordDelegatesEmailAndStoreSlug() {
        PasswordResetController controller = new PasswordResetController(passwordResetService);
        PasswordResetRequestDTO request = new PasswordResetRequestDTO();
        request.setEmail("cliente@test.com");
        request.setStoreSlug("tienda-luna");

        ResponseEntity<Map<String, String>> response = controller.forgotPassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry(
                "message",
                "Si el correo corresponde a una cuenta activa, recibirás un enlace de recuperación."
        );
        verify(passwordResetService).requestReset("cliente@test.com", "tienda-luna");
    }

    @Test
    void forgotPasswordAllowsNullRequestWithoutRevealingState() {
        PasswordResetController controller = new PasswordResetController(passwordResetService);

        ResponseEntity<Map<String, String>> response = controller.forgotPassword(null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(passwordResetService).requestReset(null, null);
    }

    @Test
    void forgotPasswordRejectsMerchantWithoutStore() {
        PasswordResetController controller = new PasswordResetController(passwordResetService);
        PasswordResetRequestDTO request = new PasswordResetRequestDTO();
        request.setEmail("merchant@test.com");
        doThrow(new BusinessRuleException("MERCHANT_WITHOUT_STORE"))
                .when(passwordResetService).requestReset("merchant@test.com", null);

        ResponseEntity<Map<String, String>> response = controller.forgotPassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody())
                .containsEntry("code", "MERCHANT_WITHOUT_STORE")
                .containsEntry(
                        "message",
                        "Tu cuenta de comerciante aún no tiene una tienda asignada. No puedes recuperar la contraseña hasta que se asocie una tienda."
                );
    }

    @Test
    void forgotPasswordKeepsGenericResponseForOtherFailures() {
        PasswordResetController controller = new PasswordResetController(passwordResetService);
        PasswordResetRequestDTO request = new PasswordResetRequestDTO();
        request.setEmail("unknown@test.com");
        doThrow(new BusinessRuleException("ACCOUNT_NOT_FOUND"))
                .when(passwordResetService).requestReset("unknown@test.com", null);

        ResponseEntity<Map<String, String>> response = controller.forgotPassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("message");
    }

    @Test
    void validatesToken() {
        PasswordResetController controller = new PasswordResetController(passwordResetService);
        when(passwordResetService.isTokenValid("token")).thenReturn(true);

        ResponseEntity<Map<String, Boolean>> response = controller.validateToken("token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("valid", true);
    }

    @Test
    void resetPasswordDelegatesRequestAndReturnsSpanishMessage() {
        PasswordResetController controller = new PasswordResetController(passwordResetService);
        PasswordResetConfirmDTO request = new PasswordResetConfirmDTO();
        request.setToken("token");
        request.setNewPassword("NuevaClave1*");

        ResponseEntity<?> response = controller.resetPassword(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of("message", "Contraseña actualizada correctamente."));
        ArgumentCaptor<PasswordResetConfirmDTO> captor = ArgumentCaptor.forClass(PasswordResetConfirmDTO.class);
        verify(passwordResetService).resetPassword(captor.capture());
        assertThat(captor.getValue()).isSameAs(request);
    }

    @Test
    void resetPasswordMapsPasswordPolicyAndInvalidTokenErrors() {
        PasswordResetController controller = new PasswordResetController(passwordResetService);
        PasswordResetConfirmDTO weak = new PasswordResetConfirmDTO();
        doThrow(new BusinessRuleException("PASSWORD_POLICY_INVALID"))
                .when(passwordResetService).resetPassword(weak);

        ResponseEntity<?> weakResponse = controller.resetPassword(weak);

        assertThat(weakResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> weakBody = (Map<?, ?>) weakResponse.getBody();
        assertThat(weakBody.get("code")).isEqualTo("PASSWORD_POLICY_INVALID");
        assertThat(weakBody.get("message"))
                .isEqualTo("La contraseña debe tener entre 8 y 72 caracteres e incluir mayúscula, minúscula, número y símbolo.");

        PasswordResetConfirmDTO expired = new PasswordResetConfirmDTO();
        doThrow(new BusinessRuleException("RESET_TOKEN_INVALID"))
                .when(passwordResetService).resetPassword(expired);

        ResponseEntity<?> expiredResponse = controller.resetPassword(expired);

        assertThat(expiredResponse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        Map<?, ?> expiredBody = (Map<?, ?>) expiredResponse.getBody();
        assertThat(expiredBody.get("code")).isEqualTo("RESET_TOKEN_INVALID");
        assertThat(expiredBody.get("message")).isEqualTo("El enlace es inválido, expiró o ya fue utilizado.");
    }
}
