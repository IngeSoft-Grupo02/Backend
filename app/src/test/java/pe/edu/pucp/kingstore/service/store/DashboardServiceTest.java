package pe.edu.pucp.kingstore.service.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.product.enums.ProductStatus;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.repository.order.OrderRepository;
import pe.edu.pucp.kingstore.repository.product.ProductRepository;
import pe.edu.pucp.kingstore.repository.quotation.QuotationRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private QuotationRepository quotationRepository;

    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(productRepository, orderRepository, quotationRepository);
    }

    @Test
    void getDashboardDataCountsPendingItemsAndBuildsRecentOrders() {
        when(productRepository.findByStoreId(10)).thenReturn(List.of(
                product(ProductStatus.DRAFT),
                product(ProductStatus.ACTIVE),
                product(ProductStatus.DRAFT)));
        when(quotationRepository.findByStoreId(10)).thenReturn(List.of(
                quotation(QuotationStatus.PENDING),
                quotation(QuotationStatus.APPROVED),
                quotation(QuotationStatus.PENDING)));

        Order paid = order(1, OrderStatus.PAYMENT_CONFIRMED, LocalDateTime.now().minusDays(1), "Ana", "Perez", "Rios");
        Order preparing = order(2, OrderStatus.IN_PREPARATION, LocalDateTime.now(), null, null, null);
        Order sent = order(3, OrderStatus.IN_TRANSIT, LocalDateTime.now().minusHours(1), "Luis", "Lopez", "Mora");
        Order delivered = order(4, OrderStatus.DELIVERED, LocalDateTime.now().minusDays(2), "Eva", "Diaz", "Paz");
        Order cancelled = order(5, OrderStatus.CANCELLED, LocalDateTime.now().minusMinutes(30), "No", "Date", "Last");
        Order extra = order(6, OrderStatus.PAYMENT_CONFIRMED, null, "Extra", "User", "Test");
        when(orderRepository.findByStoreId(10)).thenReturn(List.of(extra, delivered, paid, cancelled, sent, preparing));

        var dashboard = service.getDashboardData(10);

        assertThat(dashboard.pendingOrders()).isEqualTo(4);
        assertThat(dashboard.pendingQuotes()).isEqualTo(2);
        assertThat(dashboard.drafts()).isEqualTo(2);
        assertThat(dashboard.recentOrders()).hasSize(5);
        assertThat(dashboard.recentOrders().get(0).status()).isEqualTo("En proceso");
        assertThat(dashboard.recentOrders()).extracting("status")
                .contains("Pagado", "Enviado", "Entregado", "Cancelado");
        assertThat(dashboard.recentOrders().get(0).storeId()).isEqualTo(10);
    }

    private Product product(ProductStatus status) {
        Product product = new Product();
        product.setStatus(status);
        return product;
    }

    private Quotation quotation(QuotationStatus status) {
        Quotation quotation = new Quotation();
        quotation.setStatus(status);
        return quotation;
    }

    private Order order(int id, OrderStatus status, LocalDateTime createdAt,
                        String firstName, String paternalSurname, String maternalSurname) {
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        order.setFinalTotal(100.0 + id);
        order.setCreatedAt(createdAt);
        Quotation quotation = new Quotation();
        ShoppingCart cart = new ShoppingCart();
        Customer customer = new Customer();
        customer.setFirstName(firstName);
        customer.setPaternalSurname(paternalSurname);
        customer.setMaternalSurname(maternalSurname);
        cart.setCustomer(customer);
        quotation.setShoppingCart(cart);
        order.setQuotation(quotation);
        return order;
    }
}
