package pe.edu.pucp.kingstore.service.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import pe.edu.pucp.kingstore.domain.dto.user.PasswordResetConfirmDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.domain.model.user.PasswordResetToken;
import pe.edu.pucp.kingstore.domain.model.user.SystemAdministrator;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.CustomerRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.repository.user.PasswordResetTokenRepository;
import pe.edu.pucp.kingstore.repository.user.SystemAdministratorRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private CustomerRepository customerRepository;
    @Mock private MerchantRepository merchantRepository;
    @Mock private SystemAdministratorRepository administratorRepository;
    @Mock private StoreRepository storeRepository;
    @Mock private JavaMailSender mailSender;

    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(
                userAccountRepository,
                tokenRepository,
                customerRepository,
                merchantRepository,
                administratorRepository,
                storeRepository,
                mailSender,
                "http://localhost:3000/",
                "kingstore.test@gmail.com",
                30
        );
    }

    @Test
    void ignoresUnknownEmailWithoutRevealingAccountState() {
        when(userAccountRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        service.requestReset(" UNKNOWN@test.com ");

        verify(tokenRepository, never()).save(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void createsHashedTokenAndSendsAdminLink() {
        UserAccount account = activeAccount();
        SystemAdministrator administrator = new SystemAdministrator();
        administrator.setUserAccount(account);
        when(userAccountRepository.findByEmail(account.getEmail())).thenReturn(Optional.of(account));
        when(customerRepository.existsByUserAccountId(7)).thenReturn(false);
        when(merchantRepository.findByUserAccountId(7)).thenReturn(Optional.empty());
        when(administratorRepository.findByUserAccountId(7)).thenReturn(Optional.of(administrator));
        when(tokenRepository.findAllByUserAccountIdAndActiveTrue(7)).thenReturn(List.of());

        service.requestReset(account.getEmail());

        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(tokenCaptor.capture());
        PasswordResetToken token = tokenCaptor.getValue();
        assertThat(token.getTokenHash()).hasSize(64).doesNotContain("=");
        assertThat(token.getExpiresAt()).isAfter(token.getRequestedAt());

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        assertThat(mailCaptor.getValue().getText())
                .contains("http://localhost:3000/admin/recuperar-contrasena?token=")
                .contains("30 minutos");
    }

    @Test
    void sendsMerchantLinkForMerchantAccount() {
        UserAccount account = activeAccount();
        Merchant merchant = new Merchant();
        merchant.setUserAccount(account);
        when(userAccountRepository.findByEmail(account.getEmail())).thenReturn(Optional.of(account));
        when(customerRepository.existsByUserAccountId(7)).thenReturn(false);
        when(merchantRepository.findByUserAccountId(7)).thenReturn(Optional.of(merchant));
        Store store = new Store();
        store.setStoreName("Tienda Luna");
        when(storeRepository.findAllByMerchant_UserAccount_Id(7)).thenReturn(List.of(store));
        when(tokenRepository.findAllByUserAccountIdAndActiveTrue(7)).thenReturn(List.of());

        service.requestReset(account.getEmail());

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        assertThat(mailCaptor.getValue().getSubject())
                .isEqualTo("Kingstore - Recuperación de contraseña");
        assertThat(mailCaptor.getValue().getText())
                .contains("cambiar tu contraseña de Kingstore")
                .contains("http://localhost:3000/comerciante/recovery?token=");
    }

    @Test
    void rejectsMerchantPasswordResetWhenMerchantHasNoStores() {
        UserAccount account = activeAccount();
        Merchant merchant = new Merchant();
        merchant.setUserAccount(account);
        when(userAccountRepository.findByEmail(account.getEmail())).thenReturn(Optional.of(account));
        when(customerRepository.existsByUserAccountId(7)).thenReturn(false);
        when(merchantRepository.findByUserAccountId(7)).thenReturn(Optional.of(merchant));
        when(storeRepository.findAllByMerchant_UserAccount_Id(7)).thenReturn(List.of());

        assertThatThrownBy(() -> service.requestReset(account.getEmail()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("MERCHANT_WITHOUT_STORE");

        verify(tokenRepository, never()).save(any());
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendsCustomerLinkForCustomerAccount() {
        UserAccount account = activeAccount();
        when(userAccountRepository.findByEmail(account.getEmail())).thenReturn(Optional.of(account));
        when(customerRepository.existsByUserAccountId(7)).thenReturn(true);
        when(tokenRepository.findAllByUserAccountIdAndActiveTrue(7)).thenReturn(List.of());

        service.requestReset(account.getEmail());

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        assertThat(mailCaptor.getValue().getText())
                .contains("http://localhost:3000/recuperacion?token=");
    }

    @Test
    void sendsCustomerStoreNameWhenStoreSlugIsProvided() {
        UserAccount account = activeAccount();
        Store store = new Store();
        store.setStoreName("Tienda Luna");
        store.setSlug("tienda-luna");
        Customer customer = new Customer();
        customer.setUserAccount(account);
        customer.setStore(store);
        when(userAccountRepository.findByEmail(account.getEmail())).thenReturn(Optional.of(account));
        when(customerRepository.findByUserAccountIdAndStore_Slug(7, "tienda-luna")).thenReturn(Optional.of(customer));
        when(tokenRepository.findAllByUserAccountIdAndActiveTrue(7)).thenReturn(List.of());

        service.requestReset(account.getEmail(), " tienda-luna ");

        ArgumentCaptor<SimpleMailMessage> mailCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(mailCaptor.capture());
        assertThat(mailCaptor.getValue().getSubject())
                .isEqualTo("Tienda Luna - Recuperación de contraseña");
        assertThat(mailCaptor.getValue().getText())
                .contains("cambiar tu contraseña de Tienda Luna")
                .contains("http://localhost:3000/recuperacion?token=");
    }

    @Test
    void resetsPasswordAndConsumesToken() {
        UserAccount account = activeAccount();
        PasswordResetToken token = new PasswordResetToken();
        token.setId(9);
        token.setUserAccount(account);
        token.setTokenHash("hash");
        token.setRequestedAt(LocalDateTime.now().minusMinutes(1));
        token.setExpiresAt(LocalDateTime.now().plusMinutes(20));
        token.setActive(true);
        when(tokenRepository.findByTokenHashAndActiveTrue(any())).thenReturn(Optional.of(token));
        when(tokenRepository.findAllByUserAccountIdAndActiveTrue(7)).thenReturn(List.of(token));

        PasswordResetConfirmDTO request = new PasswordResetConfirmDTO();
        request.setToken("raw-token");
        request.setNewPassword("NuevaClave1*");
        service.resetPassword(request);

        assertThat(account.getPassword())
                .startsWith("$2")
                .isNotEqualTo("NuevaClave1*");
        assertThat(token.getActive()).isFalse();
        assertThat(token.getUsedAt()).isNotNull();
        verify(userAccountRepository).save(account);
    }

    @Test
    void rejectsWeakPasswordAndExpiredToken() {
        PasswordResetConfirmDTO weak = new PasswordResetConfirmDTO();
        weak.setToken("token");
        weak.setNewPassword("weak");
        assertThatThrownBy(() -> service.resetPassword(weak))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("PASSWORD_POLICY_INVALID");

        PasswordResetToken expired = new PasswordResetToken();
        expired.setUserAccount(activeAccount());
        expired.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        expired.setActive(true);
        when(tokenRepository.findByTokenHashAndActiveTrue(any())).thenReturn(Optional.of(expired));

        PasswordResetConfirmDTO request = new PasswordResetConfirmDTO();
        request.setToken("expired");
        request.setNewPassword("NuevaClave1*");
        assertThatThrownBy(() -> service.resetPassword(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("RESET_TOKEN_INVALID");
    }

    private UserAccount activeAccount() {
        UserAccount account = new UserAccount();
        account.setId(7);
        account.setEmail("admin@test.com");
        account.setPassword("old-password");
        account.setActive(true);
        return account;
    }

    private Store store(String storeName) {
        Store store = new Store();
        store.setStoreName(storeName);
        return store;
    }
}
