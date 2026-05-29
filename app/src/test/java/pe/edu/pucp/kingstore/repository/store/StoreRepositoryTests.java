package pe.edu.pucp.kingstore.repository.store;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.store.Store;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class StoreRepositoryTests {
    @Autowired
    private StoreRepository underTest;
    @Test
    public void testThatStoreCanBeCreatedAndRecalled(){
        Store store = underTest.save(StoreTestDataUtil.createTestStoreA());

        Optional<Store> result = underTest.findById(store.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(store.getId());
        assertThat(result.get().getSlug()).isEqualTo(store.getSlug());
        assertThat(result.get().getCategory().getId())
                .isEqualTo(store.getCategory().getId());
    }

    @Test
    public void testThatMultipleStoresCanBeCreatedAndRecalled(){
        Store storeA = underTest.save(StoreTestDataUtil.createTestStoreA());

        Store storeB = underTest.save(StoreTestDataUtil.createTestStoreB());

        Store storeC = underTest.save(StoreTestDataUtil.createTestStoreC());


        List<Store> result = underTest.findAll();
        assertThat(result)
                .hasSize(3)
                .extracting(Store::getId)
                .containsExactlyInAnyOrder(
                        storeA.getId(),
                        storeB.getId(),
                        storeC.getId()
                );
    }

    @Test
    public void testThatStoreCanBeUpdated(){
        Store store = underTest.save(StoreTestDataUtil.createTestStoreA());

        store.setDescription("updated description");

        Optional<Store> result = underTest.findById(store.getId());

        assertThat(result).isPresent();

        assertThat(result.get().getDescription())
                .isEqualTo("updated description");

        assertThat(result.get().getCategory().getId())
                .isEqualTo(store.getCategory().getId());
    }

    @Test
    public void testThatStoreCanBeDeleted(){
        Store store = underTest.save(StoreTestDataUtil.createTestStoreA());

        assertThat(underTest.findById(store.getId())).isPresent();

        underTest.deleteById(store.getId());

        Optional<Store> result = underTest.findById(store.getId());

        assertThat(result).isEmpty();

    }

    @Test
    public void testThatFindBySlugWorks(){
        Store storeA = underTest.save(StoreTestDataUtil.createTestStoreA());

        Store storeB = underTest.save(StoreTestDataUtil.createTestStoreB());

        Store storeC = underTest.save(StoreTestDataUtil.createTestStoreC());

        Optional<Store> result = underTest.findBySlug(storeA.getSlug());

        assertThat(result).isPresent();
        assertThat(result.get().getSlug()).isEqualTo(storeA.getSlug());
    }

    @Test
    public void testThatFindByActiveWorks(){
        Store storeA = underTest.save(StoreTestDataUtil.createTestStoreA());

        Store storeB = underTest.save(StoreTestDataUtil.createTestStoreB());

        Store storeC = underTest.save(StoreTestDataUtil.createTestStoreC());

        List<Store> result = underTest.findByActive(true);

        assertThat(result).hasSize(2)
                .extracting(Store::getId)
                .containsExactlyInAnyOrder(
                        storeA.getId(),
                        storeC.getId()
                );
    }
}
