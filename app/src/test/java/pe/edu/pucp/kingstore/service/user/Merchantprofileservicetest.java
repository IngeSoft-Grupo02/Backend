package pe.edu.pucp.kingstore.service.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.pucp.kingstore.domain.dto.user.MerchantPasswordRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.user.MerchantProfileRequestDTO;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre MerchantProfileService completo:
 *  - updateProfile (todas las validaciones)
 *  - updatePassword (todas las validaciones + validateNewPassword)
 *  - toResponseDTO
 */
@ExtendWith(MockitoExtension.class)
class MerchantProfileServiceTest {

    @Mock
    private MerchantRepository merchantRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    private MerchantProfileService service;

    @BeforeEach
    void setUp() {
        service = new MerchantProfileService(merchantRepository, userAccountRepository);
    }

    private UserAccount account(int id, String email, String password) {
        UserAccount account = new UserAccount();
        account.setId(id);
        account.setEmail(email);
        account.setPassword(password);
        return account;
    }

    private Merchant merchant(UserAccount account) {
        Merchant merchant = new Merchant();
        merchant.setId(1);
        merchant.setUserAccount(account);
        merchant.setFirstName("Juan");
        merchant.setPaternalSurname("Perez");
        merchant.setMaternalSurname("Lopez");
        merchant.setPhone("999999999");
        merchant.setRuc("12345678901");
        return merchant;
    }

    private MerchantProfileRequestDTO profileRequest() {
        MerchantProfileRequestDTO dto = new MerchantProfileRequestDTO();
        dto.setEmail("nuevo@mail.com");
        dto.setFirstName("Juan");
        dto.setPaternalSurname("Perez");
        dto.setMaternalSurname("Lopez");
        dto.setPhone("999999999");
        return dto;
    }

    // =========================================================================
    // updateProfile
    // =========================================================================

    @Test
    void updateProfileThrowsWhenAccountMissing() {
        Merchant merchant = merchant(null);

        assertThatThrownBy(() -> service.updateProfile(merchant, profileRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("account is not configured");
    }

    @Test
    void updateProfileThrowsWhenFirstNameOrSurnameBlank() {
        UserAccount account = account(1, "old@mail.com", "pass");
        Merchant merchant = merchant(account);

        MerchantProfileRequestDTO dto = profileRequest();
        dto.setFirstName("  ");
        assertThatThrownBy(() -> service.updateProfile(merchant, dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("First name");

        MerchantProfileRequestDTO dto2 = profileRequest();
        dto2.setPaternalSurname("  ");
        assertThatThrownBy(() -> service.updateProfile(merchant, dto2))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Paternal surname");
    }

    @Test
    void updateProfileThrowsWhenEmailFormatInvalid() {
        UserAccount account = account(1, "old@mail.com", "pass");
        Merchant merchant = merchant(account);

        MerchantProfileRequestDTO dto = profileRequest();
        dto.setEmail("correo-invalido");

        assertThatThrownBy(() -> service.updateProfile(merchant, dto))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Email format is invalid");
    }

    @Test
    void updateProfileThrowsWhenEmailAlreadyRegisteredByAnotherAccount() {
        UserAccount account = account(1, "old@mail.com", "pass");
        Merchant merchant = merchant(account);

        UserAccount other = account(2, "nuevo@mail.com", "otherpass");
        when(userAccountRepository.findByEmail("nuevo@mail.com")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.updateProfile(merchant, profileRequest()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void updateProfileAllowsSameEmailForSameAccount() {
        UserAccount account = account(1, "same@mail.com", "pass");
        Merchant merchant = merchant(account);

        MerchantProfileRequestDTO dto = profileRequest();
        dto.setEmail("same@mail.com");

        when(userAccountRepository.findByEmail("same@mail.com")).thenReturn(Optional.of(account));
        when(userAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(merchantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Merchant updated = service.updateProfile(merchant, dto);

        assertThat(updated.getUserAccount().getEmail()).isEqualTo("same@mail.com");
    }

    @Test
    void updateProfileUpdatesAllFieldsAndPersists() {
        UserAccount account = account(1, "old@mail.com", "pass");
        Merchant merchant = merchant(account);

        MerchantProfileRequestDTO dto = profileRequest();
        dto.setEmail("  Nuevo@Mail.com  ");
        dto.setFirstName("  Pedro  ");
        dto.setPaternalSurname("  Gomez  ");
        dto.setMaternalSurname("   ");
        dto.setPhone("");

        when(userAccountRepository.findByEmail("nuevo@mail.com")).thenReturn(Optional.empty());
        when(userAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(merchantRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Merchant updated = service.updateProfile(merchant, dto);

        assertThat(updated.getUserAccount().getEmail()).isEqualTo("nuevo@mail.com");
        assertThat(updated.getFirstName()).isEqualTo("Pedro");
        assertThat(updated.getPaternalSurname()).isEqualTo("Gomez");
        assertThat(updated.getMaternalSurname()).isEqualTo("");
        assertThat(updated.getPhone()).isNull();
        verify(userAccountRepository).save(account);
        verify(merchantRepository).save(merchant);
    }

    // =========================================================================
    // updatePassword
    // =========================================================================

    private MerchantPasswordRequestDTO passwordRequest(String current, String newPass, String confirm) {
        MerchantPasswordRequestDTO dto = new MerchantPasswordRequestDTO();
        dto.setCurrentPassword(current);
        dto.setNewPassword(newPass);
        dto.setConfirmPassword(confirm);
        return dto;
    }

    @Test
    void updatePasswordThrowsWhenAccountMissing() {
        Merchant merchant = merchant(null);

        assertThatThrownBy(() -> service.updatePassword(merchant, passwordRequest("a", "b", "b")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("account is not configured");
    }

    @Test
    void updatePasswordThrowsWhenFieldsBlank() {
        UserAccount account = account(1, "mail@mail.com", "old");
        Merchant merchant = merchant(account);

        assertThatThrownBy(() -> service.updatePassword(merchant, passwordRequest("", "New1234", "New1234")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Current password");

        assertThatThrownBy(() -> service.updatePassword(merchant, passwordRequest("old", "", "New1234")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("New password");

        assertThatThrownBy(() -> service.updatePassword(merchant, passwordRequest("old", "New1234", "")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("confirmation");
    }

    @Test
    void updatePasswordThrowsWhenCurrentPasswordIncorrect() {
        UserAccount account = account(1, "mail@mail.com", "correctOld");
        Merchant merchant = merchant(account);

        assertThatThrownBy(() -> service.updatePassword(merchant, passwordRequest("wrong", "New1234", "New1234")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Current password is incorrect");
    }

    @Test
    void updatePasswordThrowsWhenNewAndConfirmDoNotMatch() {
        UserAccount account = account(1, "mail@mail.com", "old");
        Merchant merchant = merchant(account);

        assertThatThrownBy(() -> service.updatePassword(merchant, passwordRequest("old", "New1234", "Other1234")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Passwords do not match");
    }

    @Test
    void updatePasswordThrowsWhenTooShort() {
        UserAccount account = account(1, "mail@mail.com", "old");
        Merchant merchant = merchant(account);

        assertThatThrownBy(() -> service.updatePassword(merchant, passwordRequest("old", "Abc123", "Abc123")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at least 8 characters");
    }

    @Test
    void updatePasswordThrowsWhenNoUppercase() {
        UserAccount account = account(1, "mail@mail.com", "old");
        Merchant merchant = merchant(account);

        assertThatThrownBy(() -> service.updatePassword(merchant, passwordRequest("old", "abcd1234", "abcd1234")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("uppercase");
    }

    @Test
    void updatePasswordThrowsWhenNoDigit() {
        UserAccount account = account(1, "mail@mail.com", "old");
        Merchant merchant = merchant(account);

        assertThatThrownBy(() -> service.updatePassword(merchant, passwordRequest("old", "Abcdefgh", "Abcdefgh")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("number");
    }

    @Test
    void updatePasswordSucceedsAndSaves() {
        UserAccount account = account(1, "mail@mail.com", "old");
        Merchant merchant = merchant(account);
        when(userAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updatePassword(merchant, passwordRequest("old", "NewPass1", "NewPass1"));

        assertThat(account.getPassword())
                .startsWith("$2")
                .isNotEqualTo("NewPass1");
        verify(userAccountRepository).save(account);
        verify(merchantRepository, never()).save(any());
    }

    // =========================================================================
    // toResponseDTO
    // =========================================================================

    @Test
    void toResponseDTOMapsAllFieldsWithAccount() {
        UserAccount account = account(1, "mail@mail.com", "pass");
        Merchant merchant = merchant(account);

        var dto = service.toResponseDTO(merchant);

        assertThat(dto.getId()).isEqualTo(1);
        assertThat(dto.getEmail()).isEqualTo("mail@mail.com");
        assertThat(dto.getName()).isEqualTo("Juan Perez Lopez");
        assertThat(dto.getFirstName()).isEqualTo("Juan");
        assertThat(dto.getPaternalSurname()).isEqualTo("Perez");
        assertThat(dto.getMaternalSurname()).isEqualTo("Lopez");
        assertThat(dto.getPhone()).isEqualTo("999999999");
        assertThat(dto.getRuc()).isEqualTo("12345678901");
    }

    @Test
    void toResponseDTOHandlesNullAccount() {
        Merchant merchant = merchant(null);

        var dto = service.toResponseDTO(merchant);

        assertThat(dto.getEmail()).isNull();
    }
}
