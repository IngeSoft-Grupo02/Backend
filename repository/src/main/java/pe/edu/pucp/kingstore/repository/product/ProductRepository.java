package pe.edu.pucp.kingstore.repository.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pe.edu.pucp.kingstore.domain.model.product.Product;

import java.util.List;

@Repository
public interface ProductRepository
    extends JpaRepository<Product, Integer> {

    @Query("""
            select p
            from Product p
            where p.store.id = :storeId
              and (p.deleted = false or p.deleted is null)
            order by p.id desc
            """)
    List<Product> findByStoreId(@Param("storeId") Integer storeId);

    @Query("""
            select p
            from Product p
            where p.store.id = :storeId
              and p.active = :active
              and (p.deleted = false or p.deleted is null)
            order by p.id desc
            """)
    List<Product> findByStoreIdAndActive(@Param("storeId") Integer storeId,
                                         @Param("active") Boolean active);

    @Query("""
            select p
            from Product p
            where lower(p.name) like lower(concat('%', :name, '%'))
              and p.store.id = :storeId
              and (p.deleted = false or p.deleted is null)
            order by p.id desc
            """)
    List<Product> findByNameContainingAndStoreId(@Param("name") String name,
                                                 @Param("storeId") Integer storeId);

    @Query("""
            select count(ci)
            from CartItem ci
            join ci.productVariant v
            where v.product.id = :productId
            """)
    long countCartItemReferences(@Param("productId") Integer productId);

    @Query("""
            select count(qi)
            from QuotationItem qi
            join qi.productVariant v
            where v.product.id = :productId
            """)
    long countQuotationItemReferences(@Param("productId") Integer productId);

    @Query("""
            select count(oi)
            from OrderItem oi
            join oi.productVariant v
            where v.product.id = :productId
            """)
    long countOrderItemReferences(@Param("productId") Integer productId);

    @Query("""
            select count(d)
            from Discount d
            where d.product.id = :productId
              and (d.deleted = false or d.deleted is null)
            """)
    long countDiscountReferences(@Param("productId") Integer productId);

}
