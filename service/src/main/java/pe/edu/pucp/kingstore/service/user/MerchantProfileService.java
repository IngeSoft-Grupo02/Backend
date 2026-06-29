package pe.edu.pucp.kingstore.service.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.dto.user.MerchantPasswordRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.user.MerchantProfileRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.user.MerchantProfileResponseDTO;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.user.util.MerchantCustomerUtil;
import pe.edu.pucp.kingstore.service.user.util.MerchantStringUtil;

import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class MerchantProfileService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final MerchantRepository    merchantRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordHashService passwordHashService = new PasswordHashService();

    public MerchantProfileService(MerchantRepository merchantRepository,
                                  UserAccountRepository userAccountRepository) {
        this.merchantRepository    = merchantRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Transactional
    public Merchant updateProfile(Merchant merchant, MerchantProfileRequestDTO request) {
        UserAccount account = merchant.getUserAccount();
        if (account == null) {
            throw new BusinessRuleException("Merchant account is not configured");
        }
        MerchantStringUtil.requireText(request.getFirstName(), "First name");
        MerchantStringUtil.requireText(request.getPaternalSurname(), "Paternal surname");

        String email = MerchantStringUtil.normalizeEmail(request.getEmail());
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new BusinessRuleException("Email format is invalid");
        }
        userAccountRepository.findByEmail(email)
                .filter(e -> !Objects.equals(e.getId(), account.getId()))
                .ifPresent(e -> {
                    throw new BusinessRuleException("Email is already registered");
                });

        account.setEmail(email);
        merchant.setFirstName(request.getFirstName().trim());
        merchant.setPaternalSurname(request.getPaternalSurname().trim());
        merchant.setMaternalSurname(MerchantStringUtil.blankToEmpty(request.getMaternalSurname()));
        merchant.setPhone(MerchantStringUtil.blankToNull(request.getPhone()));

        userAccountRepository.save(account);
        return merchantRepository.save(merchant);
    }

    @Transactional
    public void updatePassword(Merchant merchant, MerchantPasswordRequestDTO request) {
        UserAccount account = merchant.getUserAccount();
        if (account == null) {
            throw new BusinessRuleException("Merchant account is not configured");
        }
        MerchantStringUtil.requireText(request.getCurrentPassword(), "Current password");
        MerchantStringUtil.requireText(request.getNewPassword(),     "New password");
        MerchantStringUtil.requireText(request.getConfirmPassword(), "Password confirmation");

        if (!passwordHashService.matches(request.getCurrentPassword(), account.getPassword())) {
            throw new BusinessRuleException("Current password is incorrect");
        }
        if (!Objects.equals(request.getNewPassword(), request.getConfirmPassword())) {
            throw new BusinessRuleException("Passwords do not match");
        }
        validateNewPassword(request.getNewPassword());
        account.setPassword(passwordHashService.hash(request.getNewPassword()));
        userAccountRepository.save(account);
    }

    public MerchantProfileResponseDTO toResponseDTO(Merchant merchant) {
        UserAccount account = merchant.getUserAccount();
        return new MerchantProfileResponseDTO(
                merchant.getId(),
                account != null ? account.getEmail() : null,
                MerchantCustomerUtil.fullName(
                        merchant.getFirstName(),
                        merchant.getPaternalSurname(),
                        merchant.getMaternalSurname()),
                merchant.getFirstName(),
                merchant.getPaternalSurname(),
                merchant.getMaternalSurname(),
                merchant.getPhone(),
                merchant.getRuc()
        );
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private void validateNewPassword(String password) {
        if (password.length() < 8) {
            throw new BusinessRuleException(
                    "New password must have at least 8 characters");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            throw new BusinessRuleException(
                    "New password must contain at least one uppercase letter");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            throw new BusinessRuleException(
                    "New password must contain at least one number");
        }
    }
}
