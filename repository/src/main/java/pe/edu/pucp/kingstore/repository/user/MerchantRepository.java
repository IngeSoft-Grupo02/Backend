package pe.edu.pucp.kingstore.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;

import java.util.Optional;

@Repository
public interface MerchantRepository extends JpaRepository<Merchant, Integer> {

    Optional<Merchant> findByUserAccountId(Integer userAccountId);

    // Busca comerciante por el email de su UserAccount — usado en carga masiva
    Optional<Merchant> findByUserAccount_Email(String email);
}
