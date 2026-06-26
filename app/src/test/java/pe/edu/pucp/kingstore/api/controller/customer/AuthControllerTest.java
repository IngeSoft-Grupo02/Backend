package pe.edu.pucp.kingstore.api.controller.customer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import pe.edu.pucp.kingstore.domain.dto.user.LoginRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.user.LoginResponseDTO;
import pe.edu.pucp.kingstore.domain.model.user.enums.Role;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.security.JwtUtil;
import pe.edu.pucp.kingstore.service.store.StoreService;
import pe.edu.pucp.kingstore.service.user.UserAccountService;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserAccountService userAccountService;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private StoreService storeService;

    @Test
    void loginReturnsTokenForMerchantWithStoreSlug() {
        AuthController controller = new AuthController(userAccountService, jwtUtil, storeService);
        LoginRequestDTO request = new LoginRequestDTO();
        LoginResponseDTO login = response(7, "merchant@test.com", Role.MERCHANT);
        when(userAccountService.authenticate(request)).thenReturn(login);
        when(storeService.findLoginSlugByUserAccountId(7)).thenReturn(Optional.of("tienda-luna"));
        when(jwtUtil.generateToken(7, "merchant@test.com", Role.MERCHANT, "tienda-luna"))
                .thenReturn("jwt-token");

        ResponseEntity<?> response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        LoginResponseDTO body = (LoginResponseDTO) response.getBody();
        assertThat(body.getToken()).isEqualTo("jwt-token");
        assertThat(body.getStoreSlug()).isEqualTo("tienda-luna");
    }

    @Test
    void loginRejectsCustomerForMerchantPanel() {
        AuthController controller = new AuthController(userAccountService, jwtUtil, storeService);
        LoginRequestDTO request = new LoginRequestDTO();
        when(userAccountService.authenticate(request)).thenReturn(response(8, "customer@test.com", Role.CUSTOMER));

        ResponseEntity<?> response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(((Map<?, ?>) response.getBody()).get("code")).isEqualTo("ROLE_NOT_ALLOWED");
    }

    @Test
    void loginMapsKnownAuthenticationErrors() {
        AuthController controller = new AuthController(userAccountService, jwtUtil, storeService);
        LoginRequestDTO missing = new LoginRequestDTO();
        missing.setEmail("missing@test.com");
        when(userAccountService.authenticate(missing)).thenThrow(new BusinessRuleException("ACCOUNT_NOT_FOUND"));

        ResponseEntity<?> missingResponse = controller.login(missing);

        assertThat(missingResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        Map<?, ?> missingBody = (Map<?, ?>) missingResponse.getBody();
        assertThat(missingBody.get("code")).isEqualTo("ACCOUNT_NOT_FOUND");
        assertThat(missingBody.get("message")).isEqualTo("No existe una cuenta registrada con ese correo.");

        LoginRequestDTO inactive = new LoginRequestDTO();
        inactive.setEmail("inactive@test.com");
        when(userAccountService.authenticate(inactive)).thenThrow(new BusinessRuleException("ACCOUNT_INACTIVE"));

        ResponseEntity<?> inactiveResponse = controller.login(inactive);

        assertThat(inactiveResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(((Map<?, ?>) inactiveResponse.getBody()).get("code")).isEqualTo("ACCOUNT_INACTIVE");

        LoginRequestDTO badPassword = new LoginRequestDTO();
        badPassword.setEmail("bad-password@test.com");
        when(userAccountService.authenticate(badPassword)).thenThrow(new BusinessRuleException("BAD_CREDENTIALS"));
        ResponseEntity<?> badPasswordResponse = controller.login(badPassword);
        assertThat(badPasswordResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(((Map<?, ?>) badPasswordResponse.getBody()).get("code")).isEqualTo("BAD_CREDENTIALS");

        LoginRequestDTO roleMissing = new LoginRequestDTO();
        roleMissing.setEmail("role-missing@test.com");
        when(userAccountService.authenticate(roleMissing)).thenThrow(new BusinessRuleException("ROLE_NOT_ASSIGNED"));
        ResponseEntity<?> roleMissingResponse = controller.login(roleMissing);
        assertThat(roleMissingResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(((Map<?, ?>) roleMissingResponse.getBody()).get("message"))
                .isEqualTo("Esta cuenta no tiene un rol asignado.");

        LoginRequestDTO emailRequired = new LoginRequestDTO();
        emailRequired.setEmail("email-required@test.com");
        when(userAccountService.authenticate(emailRequired)).thenThrow(new BusinessRuleException("Email is required"));
        assertThat(((Map<?, ?>) controller.login(emailRequired).getBody()).get("message"))
                .isEqualTo("Ingresa tu correo electrÃƒÂ³nico.");

        LoginRequestDTO passwordRequired = new LoginRequestDTO();
        passwordRequired.setEmail("password-required@test.com");
        when(userAccountService.authenticate(passwordRequired)).thenThrow(new BusinessRuleException("Password is required"));
        assertThat(((Map<?, ?>) controller.login(passwordRequired).getBody()).get("message"))
                .isEqualTo("Ingresa tu contraseÃƒÂ±a.");

        LoginRequestDTO unknown = new LoginRequestDTO();
        unknown.setEmail("unknown-error@test.com");
        when(userAccountService.authenticate(unknown)).thenThrow(new BusinessRuleException("OTHER"));
        assertThat(((Map<?, ?>) controller.login(unknown).getBody()).get("message"))
                .isEqualTo("No se pudo iniciar sesiÃƒÂ³n. Revisa tus credenciales.");
    }

    @Test
    void loginMapsMerchantWithoutStoreToForbidden() {
        AuthController controller = new AuthController(userAccountService, jwtUtil, storeService);
        LoginRequestDTO request = new LoginRequestDTO();
        LoginResponseDTO login = response(9, "merchant@test.com", Role.MERCHANT);
        when(userAccountService.authenticate(request)).thenReturn(login);
        when(storeService.findLoginSlugByUserAccountId(9)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(((Map<?, ?>) response.getBody()).get("code")).isEqualTo("MERCHANT_NO_STORE");
    }

    private LoginResponseDTO response(Integer id, String email, Role role) {
        LoginResponseDTO dto = new LoginResponseDTO();
        dto.setId(id);
        dto.setEmail(email);
        dto.setRole(role);
        return dto;
    }
}
