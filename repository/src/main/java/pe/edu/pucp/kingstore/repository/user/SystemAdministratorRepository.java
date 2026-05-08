package pe.edu.pucp.kingstore.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.user.SystemAdministrator;

import java.util.Optional;

@Repository
public interface SystemAdministratorRepository
        extends JpaRepository<SystemAdministrator, Integer> {

    Optional<SystemAdministrator> findByUserAccountId(Integer userAccountId);
}
