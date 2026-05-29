package pe.edu.pucp.kingstore.repository.cart;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
import pe.edu.pucp.kingstore.repository.product.ProductRepository;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.CustomerRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest
public class ShoppingCartRepositoryTests {
    @Autowired
    private ShoppingCartRepository underTest;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    public void testThatShoppingCartCanBeCreatedAndRecalled(){
        UserAccount userAccount = userAccountRepository.save(CartTestDataUtil.createUserAccountA());
        Store store = storeRepository.save(CartTestDataUtil.createTestStore());
        Customer customer = CartTestDataUtil.createCustomerA(userAccount);
        customer.setStore(store);
        customer = customerRepository.save(customer);
        Product product = productRepository.save(CartTestDataUtil.createTestProduct(store));

        ShoppingCart shoppingCart = underTest.save(CartTestDataUtil.createShoppingCartA(customer, product));

        Optional<ShoppingCart> result = underTest.findById(shoppingCart.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(shoppingCart.getId());
        assertThat(result.get().getItems())
                .hasSize(shoppingCart.getItems().size());
    }

    @Test
    public void testThatMultipleShoppingCartsCanBeCreatedAndRecalled(){
        UserAccount userAccount = userAccountRepository.save(CartTestDataUtil.createUserAccountA());
        Store store = storeRepository.save(CartTestDataUtil.createTestStore());
        Customer customer = CartTestDataUtil.createCustomerA(userAccount);
        customer.setStore(store);
        customer = customerRepository.save(customer);
        Product product = productRepository.save(CartTestDataUtil.createTestProduct(store));

        ShoppingCart shoppingCartA = underTest.save(CartTestDataUtil.createShoppingCartA(customer, product));
        ShoppingCart shoppingCartB = underTest.save(CartTestDataUtil.createShoppingCartB(customer, product));
        ShoppingCart shoppingCartC = underTest.save(CartTestDataUtil.createShoppingCartC(customer, product));

        List<ShoppingCart> result = underTest.findAll();

        assertThat(result)
                .hasSize(3)
                .extracting(ShoppingCart::getId)
                .containsExactlyInAnyOrder(
                        shoppingCartA.getId(),
                        shoppingCartB.getId(),
                        shoppingCartC.getId()
                );
    }

    @Test
    public void testThatShoppingCartCanBeUpdated(){
        UserAccount userAccount = userAccountRepository.save(CartTestDataUtil.createUserAccountA());
        Store store = storeRepository.save(CartTestDataUtil.createTestStore());
        Customer customer = CartTestDataUtil.createCustomerA(userAccount);
        customer.setStore(store);
        customer = customerRepository.save(customer);
        Product product = productRepository.save(CartTestDataUtil.createTestProduct(store));

        ShoppingCart shoppingCart = underTest.save(CartTestDataUtil.createShoppingCartA(customer, product));

        shoppingCart.setDiscount(0.9);
        shoppingCart.getItems().removeFirst();

        underTest.save(shoppingCart);

        Optional<ShoppingCart> result = underTest.findById(shoppingCart.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getDiscount()).isEqualTo(shoppingCart.getDiscount());
        assertThat(result.get().getItems()).hasSize(shoppingCart.getItems().size());
    }

    @Test
    public void testThatShoppingCartCanBeDeleted(){
        UserAccount userAccount = userAccountRepository.save(CartTestDataUtil.createUserAccountA());
        Store store = storeRepository.save(CartTestDataUtil.createTestStore());
        Customer customer = CartTestDataUtil.createCustomerA(userAccount);
        customer.setStore(store);
        customer = customerRepository.save(customer);
        Product product = productRepository.save(CartTestDataUtil.createTestProduct(store));

        ShoppingCart shoppingCart = underTest.save(CartTestDataUtil.createShoppingCartA(customer, product));

        assertThat(underTest.findById(shoppingCart.getId())).isPresent();

        underTest.deleteById(shoppingCart.getId());

        Optional<ShoppingCart> result = underTest.findById(shoppingCart.getId());

        assertThat(result).isNotPresent();
    }

    @Test
    public void testThatFindByCustomerIdWorks() {
        UserAccount userAccountA = userAccountRepository.save(CartTestDataUtil.createUserAccountA());
        UserAccount userAccountB = userAccountRepository.save(CartTestDataUtil.createUserAccountB());
        Store store = storeRepository.save(CartTestDataUtil.createTestStore());
        Customer customerA = CartTestDataUtil.createCustomerA(userAccountA);
        customerA.setStore(store);
        customerA = customerRepository.save(customerA);
        Customer customerB = CartTestDataUtil.createCustomerB(userAccountB);
        customerB.setStore(store);
        customerB = customerRepository.save(customerB);
        Product product = productRepository.save(CartTestDataUtil.createTestProduct(store));

        ShoppingCart shoppingCartA = underTest.save(CartTestDataUtil.createShoppingCartA(customerA, product));
        ShoppingCart shoppingCartB = underTest.save(CartTestDataUtil.createShoppingCartB(customerB, product));

        Optional<ShoppingCart> result = underTest.findByCustomerId(customerA.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(shoppingCartA.getId());

    }
}
