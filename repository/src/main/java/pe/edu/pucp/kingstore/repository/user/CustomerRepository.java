package pe.edu.pucp.kingstore.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.user.Customer;

import java.util.List;
import java.util.Optional;
@Repository
public interface CustomerRepository
    extends JpaRepository<Customer, Integer> {

}
