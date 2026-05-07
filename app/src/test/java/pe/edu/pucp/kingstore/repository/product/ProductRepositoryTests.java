package pe.edu.pucp.kingstore.repository.product;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class ProductRepositoryTests {
    @Autowired
    private ProductRepository underTest;
    @Autowired
    private StoreRepository storeRepository;

    @Test
    public void testThatProductCanBeCreatedAndRecalled(){
        Store store = storeRepository.save(ProductTestDataUtil.createTestStoreA());

        Product product = underTest.save(ProductTestDataUtil.createTestProductA(store));

        Optional<Product> result = underTest.findById(product.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(product.getId());
        assertThat(result.get().getAttributes())
                .hasSize(product.getAttributes().size());
        assertThat(result.get().getVariants())
                .hasSize(product.getVariants().size());
    }

    @Test
    public void testThatMultipleProductsCanBeCreatedAndRecalled(){
        Store store = storeRepository.save(ProductTestDataUtil.createTestStoreA());

        Product productA = underTest.save(ProductTestDataUtil.createTestProductA(store));

        Product productB = underTest.save(ProductTestDataUtil.createTestProductB(store));

        Product productC = underTest.save(ProductTestDataUtil.createTestProductC(store));

        List<Product> result = underTest.findAll();
        assertThat(result)
                .hasSize(3)
                .extracting(Product::getId)
                .containsExactlyInAnyOrder(
                        productA.getId(),
                        productB.getId(),
                        productC.getId()
                );
    }

    @Test
    public void testThatProductCanBeUpdated(){
        Store store = storeRepository.save(ProductTestDataUtil.createTestStoreA());

        Product product = underTest.save(ProductTestDataUtil.createTestProductA(store));

        product.getAttributes().removeFirst();
        product.getVariants().removeFirst();
        product.setDescription("updated description");

        underTest.save(product);
        Optional<Product> result = underTest.findById(product.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(product.getId());
        assertThat(result.get().getAttributes())
                .hasSize(product.getAttributes().size());
        assertThat(result.get().getVariants())
                .hasSize(product.getVariants().size());
        assertThat(result.get().getDescription()).isEqualTo(product.getDescription());
    }

    @Test
    public void testThatProductCanBeDeleted(){
        Store store = storeRepository.save(ProductTestDataUtil.createTestStoreA());

        Product product = underTest.save(ProductTestDataUtil.createTestProductA(store));

        assertThat(underTest.findById(product.getId())).isPresent();

        underTest.deleteById(product.getId());

        Optional<Product> result = underTest.findById(product.getId());

        assertThat(result).isEmpty();

    }
    @Test
    public void testThatFindByStoreIdWorks(){
        Store storeA = storeRepository.save(ProductTestDataUtil.createTestStoreA());

        Store storeB = storeRepository.save(ProductTestDataUtil.createTestStoreB());

        Product productA = underTest.save(ProductTestDataUtil.createTestProductA(storeA));

        Product productB = underTest.save(ProductTestDataUtil.createTestProductB(storeA));

        Product productC = underTest.save(ProductTestDataUtil.createTestProductC(storeB));

        List<Product> result = underTest.findByStoreId(storeA.getId());

        assertThat(result)
                .hasSize(2)
                .extracting(Product::getId)
                .containsExactlyInAnyOrder(
                        productA.getId(),
                        productB.getId()
                );
    }

    @Test
    public void testThatFindByStoreIdAndActiveWorks(){
        Store storeA = storeRepository.save(ProductTestDataUtil.createTestStoreA());

        Store storeB = storeRepository.save(ProductTestDataUtil.createTestStoreB());

        Product productA = underTest.save(ProductTestDataUtil.createTestProductA(storeA));

        Product productB = underTest.save(ProductTestDataUtil.createTestProductB(storeB));

        Product productC = underTest.save(ProductTestDataUtil.createTestProductC(storeA));

        List<Product> result = underTest.findByStoreIdAndActive(storeA.getId(), false);

        assertThat(result)
                .hasSize(1)
                .extracting(Product::getId)
                .containsExactlyInAnyOrder(
                        productC.getId()
                );
    }

    @Test
    public void testThatFindByNameContainingAndStoreIdWorks(){
        Store storeA = storeRepository.save(ProductTestDataUtil.createTestStoreA());

        Store storeB = storeRepository.save(ProductTestDataUtil.createTestStoreB());

        Product productA = underTest.save(ProductTestDataUtil.createTestProductA(storeA));

        Product productB = underTest.save(ProductTestDataUtil.createTestProductB(storeB));

        Product productC = underTest.save(ProductTestDataUtil.createTestProductC(storeA));

        List<Product> result = underTest.findByNameContainingAndStoreId("ZAPATI", storeA.getId());

        assertThat(result)
                .hasSize(1)
                .extracting(Product::getId)
                .containsExactlyInAnyOrder(
                        productA.getId()
                );
    }
}
