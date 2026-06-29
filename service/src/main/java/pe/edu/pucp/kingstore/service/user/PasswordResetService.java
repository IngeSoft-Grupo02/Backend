package pe.edu.pucp.kingstore.service.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.dto.user.PasswordResetConfirmDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.domain.model.user.PasswordResetToken;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.CustomerRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.repository.user.PasswordResetTokenRepository;
import pe.edu.pucp.kingstore.repository.user.SystemAdministratorRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class PasswordResetService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordResetService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,72}$"
    );

    private final UserAccountRepository userAccountRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final CustomerRepository customerRepository;
    private final MerchantRepository merchantRepository;
    private final SystemAdministratorRepository administratorRepository;
    private final StoreRepository storeRepository;
    private final JavaMailSender mailSender;
    private final String frontendBaseUrl;
    private final String mailFrom;
    private final long expirationMinutes;
    private final PasswordHashService passwordHashService = new PasswordHashService();

    public PasswordResetService(
            UserAccountRepository userAccountRepository,
            PasswordResetTokenRepository tokenRepository,
            CustomerRepository customerRepository,
            MerchantRepository merchantRepository,
            SystemAdministratorRepository administratorRepository,
            StoreRepository storeRepository,
            JavaMailSender mailSender,
            @Value("${kingstore.password-reset.frontend-base-url:http://localhost:3000}") String frontendBaseUrl,
            @Value("${spring.mail.username:}") String mailFrom,
            @Value("${kingstore.password-reset.expiration-minutes:30}") long expirationMinutes
    ) {
        this.userAccountRepository = userAccountRepository;
        this.tokenRepository = tokenRepository;
        this.customerRepository = customerRepository;
        this.merchantRepository = merchantRepository;
        this.administratorRepository = administratorRepository;
        this.storeRepository = storeRepository;
        this.mailSender = mailSender;
        this.frontendBaseUrl = trimTrailingSlash(frontendBaseUrl);
        this.mailFrom = mailFrom;
        this.expirationMinutes = expirationMinutes;
    }

    @Transactional
    public void requestReset(String rawEmail) {
        requestReset(rawEmail, null);
    }

    @Transactional
    public void requestReset(String rawEmail, String rawStoreSlug) {
        if (rawEmail == null || rawEmail.isBlank()) {
            return;
        }

        String email = rawEmail.trim().toLowerCase();
        Optional<UserAccount> accountResult = userAccountRepository.findByEmail(email)
                .filter(account -> Boolean.TRUE.equals(account.getActive()));
        if (accountResult.isEmpty()) {
            return;
        }

        UserAccount account = accountResult.get();
        ResetTarget resetTarget = resolveResetTarget(account.getId(), rawStoreSlug);
        if (resetTarget == null) {
            LOGGER.warn("Password reset requested for account {} without an assigned role", account.getId());
            return;
        }

        invalidateActiveTokens(account.getId(), null);

        String rawToken = generateToken();
        LocalDateTime now = LocalDateTime.now();
        PasswordResetToken token = new PasswordResetToken();
        token.setUserAccount(account);
        token.setTokenHash(hashToken(rawToken));
        token.setRequestedAt(now);
        token.setExpiresAt(now.plusMinutes(expirationMinutes));
        tokenRepository.save(token);

        sendResetEmail(account.getEmail(), resetTarget.resetPath, resetTarget.brandName, rawToken);
    }

    @Transactional(readOnly = true)
    public boolean isTokenValid(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return false;
        }
        return tokenRepository.findByTokenHashAndActiveTrue(hashToken(rawToken))
                .filter(this::isUsable)
                .isPresent();
    }

    @Transactional
    public void resetPassword(PasswordResetConfirmDTO request) {
        if (request == null || request.getToken() == null || request.getToken().isBlank()) {
            throw new BusinessRuleException("RESET_TOKEN_INVALID");
        }
        if (request.getNewPassword() == null || !PASSWORD_PATTERN.matcher(request.getNewPassword()).matches()) {
            throw new BusinessRuleException("PASSWORD_POLICY_INVALID");
        }

        PasswordResetToken token = tokenRepository
                .findByTokenHashAndActiveTrue(hashToken(request.getToken()))
                .filter(this::isUsable)
                .orElseThrow(() -> new BusinessRuleException("RESET_TOKEN_INVALID"));

        UserAccount account = token.getUserAccount();
        account.setPassword(passwordHashService.hash(request.getNewPassword()));
        userAccountRepository.save(account);

        LocalDateTime usedAt = LocalDateTime.now();
        token.setUsedAt(usedAt);
        token.setActive(false);
        tokenRepository.save(token);
        invalidateActiveTokens(account.getId(), token.getId());
    }

    private boolean isUsable(PasswordResetToken token) {
        return token.getUsedAt() == null
                && token.getExpiresAt().isAfter(LocalDateTime.now())
                && Boolean.TRUE.equals(token.getUserAccount().getActive());
    }

    private void invalidateActiveTokens(Integer userAccountId, Integer tokenToKeep) {
        List<PasswordResetToken> activeTokens = tokenRepository.findAllByUserAccountIdAndActiveTrue(userAccountId);
        for (PasswordResetToken activeToken : activeTokens) {
            if (tokenToKeep == null || !tokenToKeep.equals(activeToken.getId())) {
                activeToken.setActive(false);
            }
        }
        tokenRepository.saveAll(activeTokens);
    }

    private ResetTarget resolveResetTarget(Integer userAccountId, String rawStoreSlug) {
        String storeSlug = rawStoreSlug != null ? rawStoreSlug.trim() : "";
        if (!storeSlug.isBlank()) {
            Optional<Customer> customer = customerRepository.findByUserAccountIdAndStore_Slug(userAccountId, storeSlug);
            if (customer.isPresent()) {
                return new ResetTarget("/recuperacion", resolveStoreName(customer.get().getStore()));
            }
            if (customerRepository.existsByUserAccountId(userAccountId)) {
                return null;
            }
        }
        if (customerRepository.existsByUserAccountId(userAccountId)) {
            return new ResetTarget("/recuperacion", "Kingstore");
        }
        if (merchantRepository.findByUserAccountId(userAccountId).isPresent()) {
            if (storeRepository.findAllByMerchant_UserAccount_Id(userAccountId).isEmpty()) {
                throw new BusinessRuleException("MERCHANT_WITHOUT_STORE");
            }
            return new ResetTarget("/comerciante/recovery", "Kingstore");
        }
        if (administratorRepository.findByUserAccountId(userAccountId).isPresent()) {
            return new ResetTarget("/admin/recuperar-contrasena", "Kingstore");
        }
        return null;
    }

    private String resolveStoreName(Store store) {
        if (store == null || store.getStoreName() == null || store.getStoreName().isBlank()) {
            return "Kingstore";
        }
        return store.getStoreName().trim();
    }

    private void sendResetEmail(String recipient, String resetPath, String brandName, String rawToken) {
        if (mailFrom == null || mailFrom.isBlank()) {
            throw new IllegalStateException("MAIL_USERNAME must be configured to send password reset emails");
        }

        String resetUrl = frontendBaseUrl + resetPath + "?token=" + rawToken;
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(recipient);
        message.setSubject("%s - Recuperación de contraseña".formatted(brandName));
        message.setText("""
                Recibimos una solicitud para cambiar tu contraseña de %s.

                Abre este enlace para crear una nueva contraseña:
                %s

                El enlace vence en %d minutos y solo puede utilizarse una vez.
                Si no solicitaste este cambio, ignora este mensaje.
                """.formatted(brandName, resetUrl, expirationMinutes));
        mailSender.send(message);
    }

    private static final class ResetTarget {
        private final String resetPath;
        private final String brandName;

        private ResetTarget(String resetPath, String brandName) {
            this.resetPath = resetPath;
            this.brandName = brandName;
        }
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static String trimTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:3000";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
