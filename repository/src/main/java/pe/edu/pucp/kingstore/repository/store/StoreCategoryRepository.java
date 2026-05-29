package pe.edu.pucp.kingstore.repository.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.store.StoreCategory;

@Repository
public interface StoreCategoryRepository extends JpaRepository<StoreCategory, Integer> {
}
