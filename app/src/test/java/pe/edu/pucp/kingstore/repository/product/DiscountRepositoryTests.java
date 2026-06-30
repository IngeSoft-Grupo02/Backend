package pe.edu.pucp.kingstore.repository.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.product.Discount;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class DiscountRepositoryTests {
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private DiscountRepository underTest;
    @Test
    public void testThatDiscountCanBeCreatedAndRecalled(){
        Store store = storeRepository.save(ProductTestDataUtil.createTestStoreA());

        Product product = productRepository.save(ProductTestDataUtil.createTestProductA(store));

        Discount discount = underTest.save(ProductTestDataUtil.createTestDiscountA(product));

        Optional<Discount> result =  underTest.findById(discount.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(discount.getId());
        assertThat(result.get().getDiscountPercentage()).isEqualTo(discount.getDiscountPercentage());
    }

    @Test
    public void testThatMultipleDiscountsCanBeCreatedAndRecalled(){
        Store store = storeRepository.save(ProductTestDataUtil.createTestStoreA());
        Product product = productRepository.save(ProductTestDataUtil.createTestProductA(store));

        Discount discountA = underTest.save(ProductTestDataUtil.createTestDiscountA(product));
        Discount discountB = underTest.save(ProductTestDataUtil.createTestDiscountB(product));
        Discount discountC = underTest.save(ProductTestDataUtil.createTestDiscountC(product));

        List<Discount> result = underTest.findAll();
        assertThat(result)
                .hasSize(3)
                .extracting(Discount::getId)
                .containsExactlyInAnyOrder(
                        discountA.getId(),
                        discountB.getId(),
                        discountC.getId()
                );
    }

    @Test
    public void testThatDiscountCanBeUpdated(){
        Store store = storeRepository.save(ProductTestDataUtil.createTestStoreA());
        Product product = productRepository.save(ProductTestDataUtil.createTestProductA(store));
        Discount discount = underTest.save(ProductTestDataUtil.createTestDiscountA(product));

        discount.setDiscountPercentage(99);
        underTest.save(discount);
        Optional<Discount> result =  underTest.findById(discount.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getDiscountPercentage()).isEqualTo(discount.getDiscountPercentage());

    }

    @Test
    public void testThatDiscountCanBeMarkedDeletedAndRecalled(){
        Store store = storeRepository.save(ProductTestDataUtil.createTestStoreA());
        Product product = productRepository.save(ProductTestDataUtil.createTestProductA(store));
        Discount discount = underTest.save(ProductTestDataUtil.createTestDiscountA(product));

        assertThat(underTest.findById(discount.getId())).isPresent();

        discount.setDeleted(true);
        underTest.save(discount);
        Optional<Discount> result =  underTest.findById(discount.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getDeleted()).isTrue();
    }

    @Test
    public void testThatFindByProductIdWorks(){
        Store store = storeRepository.save(ProductTestDataUtil.createTestStoreA());

        Product productA = productRepository.save(ProductTestDataUtil.createTestProductA(store));
        Product productB = productRepository.save(ProductTestDataUtil.createTestProductB(store));

        Discount discountA = underTest.save(ProductTestDataUtil.createTestDiscountA(productA));
        Discount discountB = underTest.save(ProductTestDataUtil.createTestDiscountB(productB));
        Discount discountC = underTest.save(ProductTestDataUtil.createTestDiscountC(productA));

        List<Discount> result = underTest.findByProductId(productB.getId());

        assertThat(result)
                .hasSize(1)
                        .extracting(Discount::getId)
                        .containsExactlyInAnyOrder(
                                discountB.getId()
                        );
    }

    @Test
    public void testThatFindByStoreIdDoesNotReturnLogicallyDeletedDiscounts(){
        Store store = storeRepository.save(ProductTestDataUtil.createTestStoreA());
        Product product = productRepository.save(ProductTestDataUtil.createTestProductA(store));

        Discount deleted = underTest.save(ProductTestDataUtil.createTestDiscountA(product));
        Discount remaining = underTest.save(ProductTestDataUtil.createTestDiscountB(product));

        deleted.setDeleted(true);
        underTest.save(deleted);

        List<Discount> result = underTest.findByStoreId(store.getId());

        assertThat(result)
                .extracting(Discount::getId)
                .containsExactly(remaining.getId());
    }

    @Test
    public void testThatFindByProductIdDoesNotReturnLogicallyDeletedDiscounts(){
        Store store = storeRepository.save(ProductTestDataUtil.createTestStoreA());
        Product product = productRepository.save(ProductTestDataUtil.createTestProductA(store));

        Discount deleted = underTest.save(ProductTestDataUtil.createTestDiscountA(product));
        Discount remaining = underTest.save(ProductTestDataUtil.createTestDiscountB(product));

        deleted.setDeleted(true);
        underTest.save(deleted);

        List<Discount> result = underTest.findByProductId(product.getId());

        assertThat(result)
                .extracting(Discount::getId)
                .containsExactly(remaining.getId());
    }
}
