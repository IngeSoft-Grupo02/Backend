package pe.edu.pucp.kingstore.repository.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.user.Customer;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository
    extends JpaRepository<Customer, Integer> {

    Optional<Customer> findByUserAccountId(Integer userAccountId);

    // Un UserAccount puede tener varios Customer (uno por tienda): estos accesos
    // deben ser scoped por tienda para no devolver/colisionar perfiles de otra tienda.
    boolean existsByUserAccountId(Integer userAccountId);

    List<Customer> findAllByUserAccountId(Integer userAccountId);

    Optional<Customer> findByUserAccountIdAndStore_Id(Integer userAccountId, Integer storeId);

    Optional<Customer> findByUserAccountIdAndStore_Slug(Integer userAccountId, String slug);

    // Unicidad por tienda (regla de negocio): correo y DNI no se repiten dentro de la misma tienda.
    boolean existsByStore_IdAndUserAccount_Email(Integer storeId, String email);

    boolean existsByStore_IdAndDocumentNumber(Integer storeId, String documentNumber);
}
