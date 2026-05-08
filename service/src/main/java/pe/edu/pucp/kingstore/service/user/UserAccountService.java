package pe.edu.pucp.kingstore.service.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.dto.user.LoginRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.user.LoginResponseDTO;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
import pe.edu.pucp.kingstore.domain.model.user.enums.Role;
import pe.edu.pucp.kingstore.repository.user.CustomerRepository;
import pe.edu.pucp.kingstore.repository.user.MerchantRepository;
import pe.edu.pucp.kingstore.repository.user.SystemAdministratorRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;
import pe.edu.pucp.kingstore.service.common.AbstractCrudService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

@Service
public class UserAccountService extends AbstractCrudService<UserAccount> {

    private final UserAccountRepository userAccountRepository;
    private final CustomerRepository customerRepository;
    private final MerchantRepository merchantRepository;
    private final SystemAdministratorRepository administratorRepository;

    public UserAccountService(
            UserAccountRepository userAccountRepository,
            CustomerRepository customerRepository,
            MerchantRepository merchantRepository,
            SystemAdministratorRepository administratorRepository
    ) {
        super(userAccountRepository, "User account");
        this.userAccountRepository = userAccountRepository;
        this.customerRepository = customerRepository;
        this.merchantRepository = merchantRepository;
        this.administratorRepository = administratorRepository;
    }

    @Transactional(readOnly = true)
    public LoginResponseDTO authenticate(LoginRequestDTO request) {
        if (request == null) {
            throw new BusinessRuleException("Login request is required");
        }
        requireText(request.getEmail(), "Email");
        requireText(request.getPassword(), "Password");

        UserAccount account = userAccountRepository.findByEmail(normalizeEmail(request.getEmail()))
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .filter(user -> user.getPassword().equals(request.getPassword()))
                .orElseThrow(() -> new BusinessRuleException("Invalid credentials"));

        LoginResponseDTO response = new LoginResponseDTO();
        response.setId(account.getId());
        response.setEmail(account.getEmail());
        response.setRole(resolveRole(account.getId()));
        return response;
    }

    @Override
    protected void validateForSave(UserAccount account) {
        requireText(account.getEmail(), "Email");
        requireText(account.getPassword(), "Password");
        account.setEmail(normalizeEmail(account.getEmail()));

        userAccountRepository.findByEmail(account.getEmail())
                .filter(existing -> !existing.getId().equals(account.getId()))
                .ifPresent(existing -> {
                    throw new BusinessRuleException("Email is already registered");
                });
    }

    private Role resolveRole(Integer userAccountId) {
        if (customerRepository.findByUserAccountId(userAccountId).isPresent()) {
            return Role.CUSTOMER;
        }
        if (merchantRepository.findByUserAccountId(userAccountId).isPresent()) {
            return Role.MERCHANT;
        }
        if (administratorRepository.findByUserAccountId(userAccountId).isPresent()) {
            return Role.SYSTEM_ADMIN;
        }
        throw new BusinessRuleException("User account does not have a role assigned");
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
