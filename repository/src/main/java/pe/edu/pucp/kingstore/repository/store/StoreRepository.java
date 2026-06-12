package pe.edu.pucp.kingstore.repository.store;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;

import java.util.List;
import java.util.Optional;


@Repository
public interface StoreRepository
        extends JpaRepository<Store, Integer> {
    @EntityGraph("Store.withMerchantAndCategory")
    Optional<Store> findBySlug(String slug);
    @EntityGraph("Store.withMerchantAndCategory")
    List<Store> findByActive(Boolean active);
    @EntityGraph("Store.withMerchantAndCategory")
    List<Store> findByStoreStatus(StoreStatus status);
    @EntityGraph("Store.withMerchantAndCategory")
    List<Store> findAllByMerchant_UserAccount_Id(Integer userAccountId);
    @EntityGraph("Store.withMerchantAndCategory")
    Optional<Store> findByIdAndMerchant_UserAccount_Id(Integer id, Integer userAccountId);
   // @Procedure(name = "find_active_slug_by_merchant")
    //Optional<String> findActiveSlugByMerchant(@Param("p_user_account_id")Integer userAccountId);
   @EntityGraph("Store.withMerchantAndCategory")
   List<Store> findAllByMerchant_UserAccount_IdAndStoreStatusOrderByIdAsc(
           Integer userAccountId,
           StoreStatus storeStatus
   );

}
