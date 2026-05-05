package pe.edu.pucp.kingstore.repository.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.store.Store;

import javax.swing.text.html.Option;
import java.util.List;
import java.util.Optional;


@Repository
public interface StoreRepository
        extends JpaRepository<Store, Integer> {
    Optional<Store> findBySlug(String slug);
    List<Store> findByActive(Boolean active);

}
