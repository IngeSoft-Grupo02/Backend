package pe.edu.pucp.kingstore.repository.payment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.payment.PaymentReceipt;
import pe.edu.pucp.kingstore.domain.model.payment.enums.PaymentMethod;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
import pe.edu.pucp.kingstore.repository.cart.ShoppingCartRepository;
import pe.edu.pucp.kingstore.repository.order.OrderRepository;
import pe.edu.pucp.kingstore.repository.product.ProductRepository;
import pe.edu.pucp.kingstore.repository.quotation.QuotationRepository;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.CustomerRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
public class PaymentReceiptRepositoryTests {
    @Autowired
    private PaymentReceiptRepository underTest;
    @Autowired
    private OrderRepository orderRepository;
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
    public void testThatPaymentReceiptCanBeCreatedAndRecalled(){
        UserAccount userAccount = userAccountRepository.save(PaymentTestDataUtil.createUserAccountA());
        Customer customer = customerRepository.save(PaymentTestDataUtil.createCustomerA(userAccount));
        Store store = storeRepository.save(PaymentTestDataUtil.createTestStore());
        Product product = productRepository.save(PaymentTestDataUtil.createTestProduct(store));
        ShoppingCart shoppingCart = shoppingCartRepository.save(PaymentTestDataUtil.createShoppingCartA(customer,
                product));
        Quotation quotation = quotationRepository.save(PaymentTestDataUtil.createQuotationA(shoppingCart, product));
        Order order = orderRepository.save(PaymentTestDataUtil.createOrderA(quotation, product));

        PaymentReceipt paymentReceipt = underTest.save(PaymentTestDataUtil.createPaymentReceiptA(order));

        Optional<PaymentReceipt> result = underTest.findById(paymentReceipt.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(paymentReceipt.getId());
    }

    @Test
    public void testThatMultiplePaymentReceiptsCanBeCreatedAndRecalled(){
        UserAccount userAccount = userAccountRepository.save(PaymentTestDataUtil.createUserAccountA());
        Customer customer = customerRepository.save(PaymentTestDataUtil.createCustomerA(userAccount));
        Store store = storeRepository.save(PaymentTestDataUtil.createTestStore());
        Product product = productRepository.save(PaymentTestDataUtil.createTestProduct(store));

        ShoppingCart shoppingCartA = shoppingCartRepository.save(PaymentTestDataUtil.createShoppingCartA(customer,
                product));
        ShoppingCart shoppingCartB = shoppingCartRepository.save(PaymentTestDataUtil.createShoppingCartA(customer,
                product));
        ShoppingCart shoppingCartC = shoppingCartRepository.save(PaymentTestDataUtil.createShoppingCartA(customer,
                product));

        Quotation quotationA = quotationRepository.save(PaymentTestDataUtil.createQuotationA(shoppingCartA, product));
        Quotation quotationB = quotationRepository.save(PaymentTestDataUtil.createQuotationB(shoppingCartB, product));
        Quotation quotationC = quotationRepository.save(PaymentTestDataUtil.createQuotationC(shoppingCartC, product));

        Order orderA = orderRepository.save(PaymentTestDataUtil.createOrderA(quotationA, product));
        Order orderB = orderRepository.save(PaymentTestDataUtil.createOrderB(quotationB, product));
        Order orderC = orderRepository.save(PaymentTestDataUtil.createOrderC(quotationC, product));

        PaymentReceipt paymentReceiptA = underTest.save(PaymentTestDataUtil.createPaymentReceiptA(orderA));
        PaymentReceipt paymentReceiptB = underTest.save(PaymentTestDataUtil.createPaymentReceiptB(orderB));
        PaymentReceipt paymentReceiptC =  underTest.save(PaymentTestDataUtil.createPaymentReceiptC(orderC));

        List<PaymentReceipt> result = underTest.findAll();

        assertThat(result)
                .hasSize(3)
                .extracting(PaymentReceipt::getId)
                .containsExactlyInAnyOrder(
                        paymentReceiptA.getId(),
                        paymentReceiptB.getId(),
                        paymentReceiptC.getId()
                );
    }

    @Test
    public void testThatPaymentReceiptCanBeUpdated(){
        UserAccount userAccount = userAccountRepository.save(PaymentTestDataUtil.createUserAccountA());
        Customer customer = customerRepository.save(PaymentTestDataUtil.createCustomerA(userAccount));
        Store store = storeRepository.save(PaymentTestDataUtil.createTestStore());
        Product product = productRepository.save(PaymentTestDataUtil.createTestProduct(store));
        ShoppingCart shoppingCart = shoppingCartRepository.save(PaymentTestDataUtil.createShoppingCartA(customer,
                product));
        Quotation quotation = quotationRepository.save(PaymentTestDataUtil.createQuotationA(shoppingCart, product));
        Order order = orderRepository.save(PaymentTestDataUtil.createOrderA(quotation, product));

        PaymentReceipt paymentReceipt = underTest.save(PaymentTestDataUtil.createPaymentReceiptA(order));

        paymentReceipt.setPaymentMethod(PaymentMethod.CASH_ON_DELIVERY);

        underTest.save(paymentReceipt);

        Optional<PaymentReceipt> result =  underTest.findById(paymentReceipt.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getPaymentMethod()).isEqualTo(paymentReceipt.getPaymentMethod());
    }

    @Test
    public void testThatPaymentReceiptCanBeDeleted(){
        UserAccount userAccount = userAccountRepository.save(PaymentTestDataUtil.createUserAccountA());
        Customer customer = customerRepository.save(PaymentTestDataUtil.createCustomerA(userAccount));
        Store store = storeRepository.save(PaymentTestDataUtil.createTestStore());
        Product product = productRepository.save(PaymentTestDataUtil.createTestProduct(store));
        ShoppingCart shoppingCart = shoppingCartRepository.save(PaymentTestDataUtil.createShoppingCartA(customer,
                product));
        Quotation quotation = quotationRepository.save(PaymentTestDataUtil.createQuotationA(shoppingCart, product));
        Order order = orderRepository.save(PaymentTestDataUtil.createOrderA(quotation, product));

        PaymentReceipt paymentReceipt = underTest.save(PaymentTestDataUtil.createPaymentReceiptA(order));

        assertThat(underTest.findById(paymentReceipt.getId())).isPresent();

        underTest.deleteById(paymentReceipt.getId());
        Optional<PaymentReceipt> result =  underTest.findById(order.getId());
        assertThat(result).isNotPresent();
    }

    @Test
    public void testThatFindByOrderIdWorks(){
        UserAccount userAccount = userAccountRepository.save(PaymentTestDataUtil.createUserAccountA());
        Customer customer = customerRepository.save(PaymentTestDataUtil.createCustomerA(userAccount));
        Store store = storeRepository.save(PaymentTestDataUtil.createTestStore());
        Product product = productRepository.save(PaymentTestDataUtil.createTestProduct(store));

        ShoppingCart shoppingCartA = shoppingCartRepository.save(PaymentTestDataUtil.createShoppingCartA(customer,
                product));
        ShoppingCart shoppingCartB = shoppingCartRepository.save(PaymentTestDataUtil.createShoppingCartA(customer,
                product));
        ShoppingCart shoppingCartC = shoppingCartRepository.save(PaymentTestDataUtil.createShoppingCartA(customer,
                product));

        Quotation quotationA = quotationRepository.save(PaymentTestDataUtil.createQuotationA(shoppingCartA, product));
        Quotation quotationB = quotationRepository.save(PaymentTestDataUtil.createQuotationB(shoppingCartB, product));
        Quotation quotationC = quotationRepository.save(PaymentTestDataUtil.createQuotationC(shoppingCartC, product));

        Order orderA = orderRepository.save(PaymentTestDataUtil.createOrderA(quotationA, product));
        Order orderB = orderRepository.save(PaymentTestDataUtil.createOrderB(quotationB, product));
        Order orderC = orderRepository.save(PaymentTestDataUtil.createOrderC(quotationC, product));

        PaymentReceipt paymentReceiptA = underTest.save(PaymentTestDataUtil.createPaymentReceiptA(orderA));
        PaymentReceipt paymentReceiptB = underTest.save(PaymentTestDataUtil.createPaymentReceiptB(orderB));
        PaymentReceipt paymentReceiptC = underTest.save(PaymentTestDataUtil.createPaymentReceiptC(orderC));

        Optional<PaymentReceipt> result = underTest.findByOrderId(orderA.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(paymentReceiptA.getId());
        assertThat(result.get().getId()).isNotEqualTo(paymentReceiptB.getId());
        assertThat(result.get().getId()).isNotEqualTo(paymentReceiptC.getId());
    }
}
