package pe.edu.pucp.kingstore.repository.quotation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
import pe.edu.pucp.kingstore.repository.cart.ShoppingCartRepository;
import pe.edu.pucp.kingstore.repository.product.ProductRepository;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.CustomerRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class QuotationRepositoryTests {
    @Autowired
    private QuotationRepository underTest;
    @Autowired
    private ShoppingCartRepository shoppingCartRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;

    @Test
    public void testThatQuotationCanBeCreatedAndRecalled(){
        UserAccount userAccount = userAccountRepository.save(QuotationTestDataUtil.createUserAccountA());
        Store store = storeRepository.save(QuotationTestDataUtil.createTestStore());
        Customer customer = QuotationTestDataUtil.createCustomerA(userAccount);
        customer.setStore(store);
        customer = customerRepository.save(customer);
        Product product = productRepository.save(QuotationTestDataUtil.createTestProduct(store));
        ShoppingCart shoppingCart = shoppingCartRepository.save(QuotationTestDataUtil.createShoppingCartA(customer,
                product));

        Quotation quotation = underTest.save(QuotationTestDataUtil.createQuotationA(shoppingCart, product));

        Optional<Quotation> result = underTest.findById(quotation.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(quotation.getId());
        assertThat(result.get().getItems())
                .hasSize(quotation.getItems().size());
    }
    @Test
    public void testThatMultipleQuotationsCanBeCreatedAndRecalled(){
        UserAccount userAccount = userAccountRepository.save(QuotationTestDataUtil.createUserAccountA());
        Store store = storeRepository.save(QuotationTestDataUtil.createTestStore());
        Customer customer = QuotationTestDataUtil.createCustomerA(userAccount);
        customer.setStore(store);
        customer = customerRepository.save(customer);
        Product product = productRepository.save(QuotationTestDataUtil.createTestProduct(store));

        ShoppingCart shoppingCartA = shoppingCartRepository.save(QuotationTestDataUtil.createShoppingCartA(customer,
                product));
        ShoppingCart shoppingCartB = shoppingCartRepository.save(QuotationTestDataUtil.createShoppingCartA(customer,
                product));
        ShoppingCart shoppingCartC = shoppingCartRepository.save(QuotationTestDataUtil.createShoppingCartA(customer,
                product));

        Quotation quotationA = underTest.save(QuotationTestDataUtil.createQuotationA(shoppingCartA, product));
        Quotation quotationB = underTest.save(QuotationTestDataUtil.createQuotationB(shoppingCartB, product));
        Quotation quotationC = underTest.save(QuotationTestDataUtil.createQuotationC(shoppingCartC, product));

        List<Quotation> result = underTest.findAll();

        assertThat(result)
                .hasSize(3)
                .extracting(Quotation::getId)
                .containsExactlyInAnyOrder(
                        quotationA.getId(),
                        quotationB.getId(),
                        quotationC.getId()
                );
    }

    @Test
    public void testThatQuotationCanBeUpdated(){
        UserAccount userAccount = userAccountRepository.save(QuotationTestDataUtil.createUserAccountA());
        Store store = storeRepository.save(QuotationTestDataUtil.createTestStore());
        Customer customer = QuotationTestDataUtil.createCustomerA(userAccount);
        customer.setStore(store);
        customer = customerRepository.save(customer);
        Product product = productRepository.save(QuotationTestDataUtil.createTestProduct(store));
        ShoppingCart shoppingCart = shoppingCartRepository.save(QuotationTestDataUtil.createShoppingCartA(customer,
                product));

        Quotation quotation = underTest.save(QuotationTestDataUtil.createQuotationA(shoppingCart, product));

        quotation.getItems().removeFirst();
        quotation.setDescription("updated description");
        underTest.save(quotation);

        Optional<Quotation> result = underTest.findById(quotation.getId());
        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(quotation.getId());
        assertThat(result.get().getDescription()).isEqualTo(quotation.getDescription());
        assertThat(result.get().getItems()).hasSize(quotation.getItems().size());
    }

    @Test
    public void testThatQuotationCanBeDeleted(){
        UserAccount userAccount = userAccountRepository.save(QuotationTestDataUtil.createUserAccountA());
        Store store = storeRepository.save(QuotationTestDataUtil.createTestStore());
        Customer customer = QuotationTestDataUtil.createCustomerA(userAccount);
        customer.setStore(store);
        customer = customerRepository.save(customer);
        Product product = productRepository.save(QuotationTestDataUtil.createTestProduct(store));
        ShoppingCart shoppingCart = shoppingCartRepository.save(QuotationTestDataUtil.createShoppingCartA(customer,
                product));

        Quotation quotation = underTest.save(QuotationTestDataUtil.createQuotationA(shoppingCart, product));

        assertThat(underTest.findById(quotation.getId())).isPresent();

        underTest.deleteById(quotation.getId());
        Optional<Quotation> result = underTest.findById(quotation.getId());
        assertThat(result).isNotPresent();
    }

    @Test
    public void testThatFindByShoppingCartIdWorks(){
        UserAccount userAccount = userAccountRepository.save(QuotationTestDataUtil.createUserAccountA());
        Store store = storeRepository.save(QuotationTestDataUtil.createTestStore());
        Customer customer = QuotationTestDataUtil.createCustomerA(userAccount);
        customer.setStore(store);
        customer = customerRepository.save(customer);
        Product product = productRepository.save(QuotationTestDataUtil.createTestProduct(store));

        ShoppingCart shoppingCartA = shoppingCartRepository.save(QuotationTestDataUtil.createShoppingCartA(customer,
                product));
        ShoppingCart shoppingCartB = shoppingCartRepository.save(QuotationTestDataUtil.createShoppingCartB(customer,
                product));
        ShoppingCart shoppingCartC = shoppingCartRepository.save(QuotationTestDataUtil.createShoppingCartC(customer,
                product));

        Quotation quotationA = underTest.save(QuotationTestDataUtil.createQuotationA(shoppingCartA, product));
        Quotation quotationB = underTest.save(QuotationTestDataUtil.createQuotationB(shoppingCartB, product));
        Quotation quotationC = underTest.save(QuotationTestDataUtil.createQuotationC(shoppingCartC, product));

        Optional<Quotation> result = underTest.findByShoppingCartId(shoppingCartA.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(quotationA.getId());
    }

    @Test
    public void testThatFindByStatusWorks(){
        UserAccount userAccount = userAccountRepository.save(QuotationTestDataUtil.createUserAccountA());
        Store store = storeRepository.save(QuotationTestDataUtil.createTestStore());
        Customer customer = QuotationTestDataUtil.createCustomerA(userAccount);
        customer.setStore(store);
        customer = customerRepository.save(customer);
        Product product = productRepository.save(QuotationTestDataUtil.createTestProduct(store));

        ShoppingCart shoppingCartA = shoppingCartRepository.save(QuotationTestDataUtil.createShoppingCartA(customer,
                product));
        ShoppingCart shoppingCartB = shoppingCartRepository.save(QuotationTestDataUtil.createShoppingCartB(customer,
                product));
        ShoppingCart shoppingCartC = shoppingCartRepository.save(QuotationTestDataUtil.createShoppingCartC(customer,
                product));

        Quotation quotationA = underTest.save(QuotationTestDataUtil.createQuotationA(shoppingCartA, product));
        Quotation quotationB = underTest.save(QuotationTestDataUtil.createQuotationB(shoppingCartB, product));
        Quotation quotationC = underTest.save(QuotationTestDataUtil.createQuotationC(shoppingCartC, product));

        List<Quotation> result = underTest.findByStatus(QuotationStatus.PENDING);

        assertThat(result)
                .hasSize(2)
                .extracting(Quotation::getId)
                .containsExactlyInAnyOrder(
                        quotationA.getId(),
                        quotationB.getId()
                );
    }

    @Test
    public void findByCustomerIdAndStoreIdIsolatesQuotationsBetweenStoresOfSameAccount(){
        // Un mismo UserAccount global con dos membresías (Customer) en dos tiendas distintas.
        UserAccount account = userAccountRepository.save(QuotationTestDataUtil.createUserAccountA());

        Store storeA = storeRepository.save(QuotationTestDataUtil.createTestStore());
        Store storeBSeed = QuotationTestDataUtil.createTestStore();
        storeBSeed.setStoreName("Urban Threads");
        storeBSeed.setSlug("urban-threads-iso");
        storeBSeed.setCategory(storeA.getCategory());
        Store storeB = storeRepository.save(storeBSeed);

        Customer customerA = QuotationTestDataUtil.createCustomerA(account);
        customerA.setStore(storeA);
        customerA = customerRepository.save(customerA);

        Customer customerB = QuotationTestDataUtil.createCustomerA(account);
        customerB.setStore(storeB);
        customerB = customerRepository.save(customerB);

        Product product = productRepository.save(QuotationTestDataUtil.createTestProduct(storeA));
        ShoppingCart cartA = shoppingCartRepository.save(QuotationTestDataUtil.createShoppingCartA(customerA, product));
        Quotation quotationA = underTest.save(QuotationTestDataUtil.createQuotationA(cartA, product));

        // La cotización creada en storeA SOLO la ve el Customer de storeA.
        assertThat(underTest.findByCustomerIdAndStoreId(customerA.getId(), storeA.getId()))
                .extracting(Quotation::getId)
                .containsExactly(quotationA.getId());

        // El mismo UserAccount en storeB (otro Customer.id) NO ve la actividad de storeA.
        assertThat(underTest.findByCustomerIdAndStoreId(customerB.getId(), storeB.getId()))
                .isEmpty();
    }
}
