package pe.edu.pucp.kingstore.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAccountRepository
        extends JpaRepository<UserAccount, Integer> {
    Optional<UserAccount> findByEmail(String email);
    boolean existsByEmail(String email);
}
