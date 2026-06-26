package pe.edu.pucp.kingstore.api.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import pe.edu.pucp.kingstore.api.context.MerchantContext;
import pe.edu.pucp.kingstore.domain.dto.user.MerchantPasswordRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.user.MerchantProfileRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.user.MerchantProfileResponseDTO;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.user.MerchantProfileService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MerchantControllerTest {

    @Mock
    private MerchantContext merchantContext;
    @Mock
    private MerchantProfileService merchantProfileService;
    @Mock
    private Authentication authentication;

    @Test
    void profileReturnsCurrentMerchantResponse() {
        MerchantController controller = new MerchantController(merchantContext, merchantProfileService);
        Merchant merchant = new Merchant();
        MerchantProfileResponseDTO dto = new MerchantProfileResponseDTO(
                1, "merchant@test.com", "Ana Perez", "Ana", "Perez", "Rojas", "999999999", "12345678901"
        );
        when(merchantContext.merchant(authentication)).thenReturn(merchant);
        when(merchantProfileService.toResponseDTO(merchant)).thenReturn(dto);

        ResponseEntity<?> response = controller.profile(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dto);
    }

    @Test
    void updateProfileDelegatesCurrentMerchantAndRequest() {
        MerchantController controller = new MerchantController(merchantContext, merchantProfileService);
        Merchant current = new Merchant();
        Merchant updated = new Merchant();
        MerchantProfileRequestDTO request = new MerchantProfileRequestDTO();
        MerchantProfileResponseDTO dto = new MerchantProfileResponseDTO(
                1, "merchant@test.com", "Ana Perez", "Ana", "Perez", "Rojas", "999999999", "12345678901"
        );
        when(merchantContext.merchant(authentication)).thenReturn(current);
        when(merchantProfileService.updateProfile(current, request)).thenReturn(updated);
        when(merchantProfileService.toResponseDTO(updated)).thenReturn(dto);

        ResponseEntity<?> response = controller.updateProfile(authentication, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(dto);
    }

    @Test
    void updatePasswordDelegatesCurrentMerchantAndReturnsMessage() {
        MerchantController controller = new MerchantController(merchantContext, merchantProfileService);
        Merchant merchant = new Merchant();
        MerchantPasswordRequestDTO request = new MerchantPasswordRequestDTO();
        request.setCurrentPassword("Actual123*");
        request.setNewPassword("Nueva123*");
        request.setConfirmPassword("Nueva123*");
        when(merchantContext.merchant(authentication)).thenReturn(merchant);

        ResponseEntity<?> response = controller.updatePassword(authentication, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Map.of("message", "Password updated successfully"));
        verify(merchantProfileService).updatePassword(merchant, request);
    }

    @Test
    void updatePasswordMapsBusinessRuleErrors() {
        MerchantController controller = new MerchantController(merchantContext, merchantProfileService);
        Merchant merchant = new Merchant();
        MerchantPasswordRequestDTO request = new MerchantPasswordRequestDTO();
        when(merchantContext.merchant(authentication)).thenReturn(merchant);
        doThrow(new BusinessRuleException("Current password is incorrect"))
                .when(merchantProfileService).updatePassword(merchant, request);

        ResponseEntity<?> response = controller.updatePassword(authentication, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(Map.of("error", "Current password is incorrect"));
    }
}
