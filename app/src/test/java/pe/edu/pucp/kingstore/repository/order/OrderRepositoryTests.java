package pe.edu.pucp.kingstore.repository.order;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
import pe.edu.pucp.kingstore.repository.cart.ShoppingCartRepository;
import pe.edu.pucp.kingstore.repository.product.ProductRepository;
import pe.edu.pucp.kingstore.repository.quotation.QuotationRepository;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.CustomerRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest
public class OrderRepositoryTests {
    @Autowired
    private OrderRepository underTest;
    @Autowired
    private QuotationRepository quotationRepository;
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
    public void testThatOrderCanBeCreatedAndRecalled(){
        UserAccount userAccount = userAccountRepository.save(OrderTestDataUtil.createUserAccountA());
        Store store = storeRepository.save(OrderTestDataUtil.createTestStore());
        Customer customer = OrderTestDataUtil.createCustomerA(userAccount);
        customer.setStore(store);
        customer = customerRepository.save(customer);
        Product product = productRepository.save(OrderTestDataUtil.createTestProduct(store));
        ShoppingCart shoppingCart = shoppingCartRepository.save(OrderTestDataUtil.createShoppingCartA(customer,
                product));
        Quotation quotation = quotationRepository.save(OrderTestDataUtil.createQuotationA(shoppingCart, product));

        Order order = underTest.save(OrderTestDataUtil.createOrderA(quotation, product));

        Optional<Order> result = underTest.findById(order.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(order.getId());
        assertThat(result.get().getItems())
                .hasSize(order.getItems().size());
        assertThat(result.get().getShippingDetail()).isEqualTo(order.getShippingDetail());
    }
    @Test
    public void testThatMultipleOrdersCanBeCreatedAndRecalled(){
        UserAccount userAccount = userAccountRepository.save(OrderTestDataUtil.createUserAccountA());
        Store store = storeRepository.save(OrderTestDataUtil.createTestStore());
        Customer customer = OrderTestDataUtil.createCustomerA(userAccount);
        customer.setStore(store);
        customer = customerRepository.save(customer);
        Product product = productRepository.save(OrderTestDataUtil.createTestProduct(store));

        ShoppingCart shoppingCartA = shoppingCartRepository.save(OrderTestDataUtil.createShoppingCartA(customer,
                product));
        ShoppingCart shoppingCartB = shoppingCartRepository.save(OrderTestDataUtil.createShoppingCartA(customer,
                product));
        ShoppingCart shoppingCartC = shoppingCartRepository.save(OrderTestDataUtil.createShoppingCartA(customer,
                product));

        Quotation quotationA = quotationRepository.save(OrderTestDataUtil.createQuotationA(shoppingCartA, product));
        Quotation quotationB = quotationRepository.save(OrderTestDataUtil.createQuotationB(shoppingCartB, product));
        Quotation quotationC = quotationRepository.save(OrderTestDataUtil.createQuotationC(shoppingCartC, product));

        Order orderA = underTest.save(OrderTestDataUtil.createOrderA(quotationA, product));
        Order orderB = underTest.save(OrderTestDataUtil.createOrderB(quotationB, product));
        Order orderC = underTest.save(OrderTestDataUtil.createOrderC(quotationC, product));

        List<Order> result = underTest.findAll();

        assertThat(result)
                .hasSize(3)
                .extracting(Order::getId)
                .containsExactlyInAnyOrder(
                        orderA.getId(),
                        orderB.getId(),
                        orderC.getId()
                );
    }

    @Test
    public void testThatOrderCanBeUpdated(){
        UserAccount userAccount = userAccountRepository.save(OrderTestDataUtil.createUserAccountA());
        Store store = storeRepository.save(OrderTestDataUtil.createTestStore());
        Customer customer = OrderTestDataUtil.createCustomerA(userAccount);
        customer.setStore(store);
        customer = customerRepository.save(customer);
        Product product = productRepository.save(OrderTestDataUtil.createTestProduct(store));
        ShoppingCart shoppingCart = shoppingCartRepository.save(OrderTestDataUtil.createShoppingCartA(customer,
                product));
        Quotation quotation = quotationRepository.save(OrderTestDataUtil.createQuotationA(shoppingCart, product));

        Order order = underTest.save(OrderTestDataUtil.createOrderA(quotation, product));

        order.getItems().removeFirst();
        order.setStatus(OrderStatus.PAYMENT_CONFIRMED);

        underTest.save(order);
        Optional<Order> result = underTest.findById(order.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(order.getStatus());
        assertThat(result.get().getItems()).hasSize(order.getItems().size());
    }

    @Test
    public void testThatOrderCanBeDeleted(){
        UserAccount userAccount = userAccountRepository.save(OrderTestDataUtil.createUserAccountA());
        Store store = storeRepository.save(OrderTestDataUtil.createTestStore());
        Customer customer = OrderTestDataUtil.createCustomerA(userAccount);
        customer.setStore(store);
        customer = customerRepository.save(customer);
        Product product = productRepository.save(OrderTestDataUtil.createTestProduct(store));
        ShoppingCart shoppingCart = shoppingCartRepository.save(OrderTestDataUtil.createShoppingCartA(customer,
                product));
        Quotation quotation = quotationRepository.save(OrderTestDataUtil.createQuotationA(shoppingCart, product));

        Order order = underTest.save(OrderTestDataUtil.createOrderA(quotation, product));

        assertThat(underTest.findById(order.getId())).isPresent();

        underTest.deleteById(order.getId());
        Optional<Order> result =  underTest.findById(order.getId());
        assertThat(result).isNotPresent();
    }

    @Test
    public void testThatFindByQuotationIdWorks(){
        UserAccount userAccount = userAccountRepository.save(OrderTestDataUtil.createUserAccountA());
        Store store = storeRepository.save(OrderTestDataUtil.createTestStore());
        Customer customer = OrderTestDataUtil.createCustomerA(userAccount);
        customer.setStore(store);
        customer = customerRepository.save(customer);
        Product product = productRepository.save(OrderTestDataUtil.createTestProduct(store));

        ShoppingCart shoppingCartA = shoppingCartRepository.save(OrderTestDataUtil.createShoppingCartA(customer,
                product));
        ShoppingCart shoppingCartB = shoppingCartRepository.save(OrderTestDataUtil.createShoppingCartA(customer,
                product));
        ShoppingCart shoppingCartC = shoppingCartRepository.save(OrderTestDataUtil.createShoppingCartA(customer,
                product));

        Quotation quotationA = quotationRepository.save(OrderTestDataUtil.createQuotationA(shoppingCartA, product));
        Quotation quotationB = quotationRepository.save(OrderTestDataUtil.createQuotationB(shoppingCartB, product));
        Quotation quotationC = quotationRepository.save(OrderTestDataUtil.createQuotationC(shoppingCartC, product));

        Order orderA = underTest.save(OrderTestDataUtil.createOrderA(quotationA, product));
        Order orderB = underTest.save(OrderTestDataUtil.createOrderB(quotationB, product));
        Order orderC = underTest.save(OrderTestDataUtil.createOrderC(quotationC, product));

        Optional<Order> result = underTest.findByQuotationId(quotationA.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(orderA.getId());
    }

    @Test
    public void testThatFindByStatusWorks(){
        UserAccount userAccount = userAccountRepository.save(OrderTestDataUtil.createUserAccountA());
        Store store = storeRepository.save(OrderTestDataUtil.createTestStore());
        Customer customer = OrderTestDataUtil.createCustomerA(userAccount);
        customer.setStore(store);
        customer = customerRepository.save(customer);
        Product product = productRepository.save(OrderTestDataUtil.createTestProduct(store));

        ShoppingCart shoppingCartA = shoppingCartRepository.save(OrderTestDataUtil.createShoppingCartA(customer,
                product));
        ShoppingCart shoppingCartB = shoppingCartRepository.save(OrderTestDataUtil.createShoppingCartA(customer,
                product));
        ShoppingCart shoppingCartC = shoppingCartRepository.save(OrderTestDataUtil.createShoppingCartA(customer,
                product));

        Quotation quotationA = quotationRepository.save(OrderTestDataUtil.createQuotationA(shoppingCartA, product));
        Quotation quotationB = quotationRepository.save(OrderTestDataUtil.createQuotationB(shoppingCartB, product));
        Quotation quotationC = quotationRepository.save(OrderTestDataUtil.createQuotationC(shoppingCartC, product));

        Order orderA = underTest.save(OrderTestDataUtil.createOrderA(quotationA, product));
        Order orderB = underTest.save(OrderTestDataUtil.createOrderB(quotationB, product));
        Order orderC = underTest.save(OrderTestDataUtil.createOrderC(quotationC, product));

        List<Order> result = underTest.findByStatus(OrderStatus.IN_PREPARATION);

        assertThat(result)
                .hasSize(2)
                .extracting(Order::getId)
                .containsExactlyInAnyOrder(
                        orderA.getId(),
                        orderB.getId()
                );
    }

    @Test
    public void findByCustomerIsolatesOrdersBetweenStoresOfSameAccount(){
        // Mismo UserAccount con membresía (Customer) en dos tiendas: la actividad de
        // pedidos no debe mezclarse (la query va por el Customer.id, que es por tienda).
        UserAccount account = userAccountRepository.save(OrderTestDataUtil.createUserAccountA());

        Store storeA = storeRepository.save(OrderTestDataUtil.createTestStore());
        Store storeBSeed = OrderTestDataUtil.createTestStore();
        storeBSeed.setStoreName("Urban Threads");
        storeBSeed.setSlug("urban-threads-iso");
        storeBSeed.setCategory(storeA.getCategory());
        Store storeB = storeRepository.save(storeBSeed);

        Customer customerA = OrderTestDataUtil.createCustomerA(account);
        customerA.setStore(storeA);
        customerA = customerRepository.save(customerA);

        Customer customerB = OrderTestDataUtil.createCustomerA(account);
        customerB.setStore(storeB);
        customerB = customerRepository.save(customerB);

        Product product = productRepository.save(OrderTestDataUtil.createTestProduct(storeA));
        ShoppingCart cartA = shoppingCartRepository.save(OrderTestDataUtil.createShoppingCartA(customerA, product));
        Quotation quotationA = quotationRepository.save(OrderTestDataUtil.createQuotationA(cartA, product));
        Order orderA = underTest.save(OrderTestDataUtil.createOrderA(quotationA, product));

        // Pedido de storeA visible solo para el Customer de storeA.
        assertThat(underTest.findByQuotation_ShoppingCart_Customer_Id(customerA.getId()))
                .extracting(Order::getId)
                .containsExactly(orderA.getId());

        // El mismo UserAccount en storeB (otro Customer.id) no ve el pedido de storeA.
        assertThat(underTest.findByQuotation_ShoppingCart_Customer_Id(customerB.getId()))
                .isEmpty();
    }
}
