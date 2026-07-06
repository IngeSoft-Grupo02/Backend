package pe.edu.pucp.kingstore.service.order;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.order.OrderItem;
import pe.edu.pucp.kingstore.domain.model.order.ShippingDetail;
import pe.edu.pucp.kingstore.domain.model.order.enums.District;
import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.repository.order.OrderRepository;
import pe.edu.pucp.kingstore.repository.quotation.QuotationRepository;
import pe.edu.pucp.kingstore.domain.dto.order.ShippingAddressRequestDTO;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Cubre todo OrderService:
 *  - findByQuotation, findByStatus, changeStatus
 *  - findByStoreId, findByStoreIdAndStatus, findInStore
 *  - validateForSave / recalculateTotals (todos los branches)
 *  - toResponseDTO (con/sin items, con/sin shipping, con/sin cliente, todos los statusLabel)
 *  - toItemResponseDTO (con/sin variant, con/sin product)
 *  - toShippingResponseDTO (null y con datos)
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private QuotationRepository quotationRepository;

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(orderRepository, quotationRepository);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private Quotation quotation(int id) {
        Quotation q = new Quotation();
        q.setId(id);
        return q;
    }

    private Order order(Integer id, Quotation quotation, OrderStatus status) {
        Order order = new Order();
        order.setId(id);
        order.setQuotation(quotation);
        order.setStatus(status);
        order.setTotalDiscount(0.0);
        return order;
    }

    private OrderItem item(double price, int quantity) {
        OrderItem item = new OrderItem();
        item.setUnitPrice(price);
        item.setQuantity(quantity);
        return item;
    }

    // =========================================================================
    // findByQuotation
    // =========================================================================

    @Test
    void findByQuotationReturnsOrderWhenExists() {
        Order order = order(1, quotation(5), OrderStatus.PAYMENT_CONFIRMED);
        when(orderRepository.findByQuotationId(5)).thenReturn(Optional.of(order));

        assertThat(service.findByQuotation(5)).contains(order);
    }

    @Test
    void findByQuotationThrowsWhenIdInvalid() {
        assertThatThrownBy(() -> service.findByQuotation(0)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.findByQuotation(null)).isInstanceOf(BusinessRuleException.class);
    }

    // =========================================================================
    // findByStatus
    // =========================================================================

    @Test
    void findByStatusReturnsOrders() {
        Order order = order(1, quotation(5), OrderStatus.IN_PREPARATION);
        when(orderRepository.findByStatus(OrderStatus.IN_PREPARATION)).thenReturn(List.of(order));

        assertThat(service.findByStatus(OrderStatus.IN_PREPARATION)).containsExactly(order);
    }

    @Test
    void findByStatusThrowsWhenNull() {
        assertThatThrownBy(() -> service.findByStatus(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("status is required");
    }

    // =========================================================================
    // changeStatus
    // =========================================================================

    @Test
    void changeStatusThrowsWhenStatusNull() {
        assertThatThrownBy(() -> service.changeStatus(1, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("status is required");
    }

    @Test
    void changeStatusUpdatesAndSaves() {
        Order order = order(1, quotation(5), OrderStatus.PAYMENT_CONFIRMED);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.changeStatus(1, OrderStatus.IN_TRANSIT);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.IN_TRANSIT);
    }

    // =========================================================================
    // findByStoreId / findByStoreIdAndStatus / findInStore
    // =========================================================================

    @Test
    void findByStoreIdReturnsOrders() {
        Order order = order(1, quotation(5), OrderStatus.DELIVERED);
        when(orderRepository.findByStoreId(10)).thenReturn(List.of(order));

        assertThat(service.findByStoreId(10)).containsExactly(order);
    }

    @Test
    void findByStoreIdThrowsWhenIdInvalid() {
        assertThatThrownBy(() -> service.findByStoreId(0)).isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void findByStoreIdAndStatusReturnsOrders() {
        Order order = order(1, quotation(5), OrderStatus.CANCELLED);
        when(orderRepository.findByStoreIdAndStatus(10, OrderStatus.CANCELLED))
                .thenReturn(List.of(order));

        assertThat(service.findByStoreIdAndStatus(10, OrderStatus.CANCELLED)).containsExactly(order);
    }

    @Test
    void findByStoreIdAndStatusThrowsWhenStatusNull() {
        assertThatThrownBy(() -> service.findByStoreIdAndStatus(10, null))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void findInStoreReturnsOrderWhenPresent() {
        Order order = order(7, quotation(5), OrderStatus.DELIVERED);
        when(orderRepository.findByStoreId(10)).thenReturn(List.of(order));

        assertThat(service.findInStore(7, 10)).isEqualTo(order);
    }

    @Test
    void findInStoreThrowsNotFoundWhenAbsent() {
        Order order = order(7, quotation(5), OrderStatus.DELIVERED);
        when(orderRepository.findByStoreId(10)).thenReturn(List.of(order));

        assertThatThrownBy(() -> service.findInStore(99, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findInStoreThrowsWhenIdsInvalid() {
        assertThatThrownBy(() -> service.findInStore(0, 10)).isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.findInStore(1, 0)).isInstanceOf(BusinessRuleException.class);
    }

    // =========================================================================
    // validateForSave / recalculateTotals
    // =========================================================================

    @Test
    void createThrowsWhenQuotationMissing() {
        Order order = order(null, null, null);

        assertThatThrownBy(() -> service.create(order))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("must belong to a quotation");
    }

    @Test
    void createThrowsWhenQuotationIdNull() {
        Order order = order(null, new Quotation(), null);

        assertThatThrownBy(() -> service.create(order))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("must belong to a quotation");
    }

    @Test
    void createDefaultsStatusToPendingPaymentAndRecalculates() {
        OrderItem item = item(10.0, 2);
        Order order = order(null, quotation(5), null);
        order.setItems(new ArrayList<>(List.of(item)));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order saved = service.create(order);

        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(item.getSubTotal()).isEqualTo(20.0);
        assertThat(saved.getPartialTotal()).isEqualTo(20.0);
        assertThat(saved.getFinalTotal()).isEqualTo(20.0);
        assertThat(saved.getTotalDiscount()).isEqualTo(0.0);
    }

    @Test
    void recalculateTotalsThrowsWhenQuantityInvalid() {
        OrderItem item = item(10.0, 0);
        Order order = order(null, quotation(5), null);
        order.setItems(List.of(item));

        assertThatThrownBy(() -> service.create(order))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("quantity must be positive");

        OrderItem nullQty = new OrderItem();
        nullQty.setUnitPrice(10.0);
        nullQty.setQuantity(null);
        order.setItems(List.of(nullQty));
        assertThatThrownBy(() -> service.create(order))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void recalculateTotalsThrowsWhenPriceNegative() {
        OrderItem item = item(-1.0, 1);
        Order order = order(null, quotation(5), null);
        order.setItems(List.of(item));

        assertThatThrownBy(() -> service.create(order))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("price cannot be negative");
    }

    @Test
    void recalculateTotalsThrowsWhenDiscountOutOfRange() {
        OrderItem item = item(10.0, 1);
        Order negDiscount = order(null, quotation(5), null);
        negDiscount.setItems(List.of(item));
        negDiscount.setTotalDiscount(-1.0);

        assertThatThrownBy(() -> service.create(negDiscount))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("discount must be between");

        Order highDiscount = order(null, quotation(5), null);
        highDiscount.setItems(List.of(item(10.0, 1)));
        highDiscount.setTotalDiscount(100.0);
        assertThatThrownBy(() -> service.create(highDiscount))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void recalculateTotalsHandlesNullItems() {
        Order order = order(null, quotation(5), null);
        order.setItems(null);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order saved = service.create(order);

        assertThat(saved.getPartialTotal()).isEqualTo(0.0);
        assertThat(saved.getFinalTotal()).isEqualTo(0.0);
    }

    // =========================================================================
    // toResponseDTO — statusLabel y todos los estados
    // =========================================================================

    @Test
    void toResponseDTOCoversAllStatusLabels() {
        for (OrderStatus status : OrderStatus.values()) {
            Order order = order(1, quotation(5), status);
            order.setItems(null);
            order.setFinalTotal(0.0);
            var dto = service.toResponseDTO(order, 10);
            assertThat(dto.getStatusLabel()).isNotNull();
        }
    }

    @Test
    void toResponseDTOMapsAllFieldsWithCustomerAndItems() {
        Customer customer = new Customer();
        customer.setFirstName("Ana");
        customer.setPaternalSurname("Lopez");
        customer.setMaternalSurname("Diaz");

        ShoppingCart cart = new ShoppingCart();
        cart.setCustomer(customer);

        Quotation quotation = quotation(5);
        quotation.setShoppingCart(cart);
        quotation.setObservations("Enviar pronto");

        ProductVariant variant = new ProductVariant();
        variant.setId(1);
        variant.setSize("M");
        variant.setColor(Color.BLACK);
        variant.setStock(5);

        OrderItem item = new OrderItem();
        item.setProductVariant(variant);
        item.setQuantity(2);
        item.setUnitPrice(50.0);
        item.setSubTotal(100.0);

        ShippingDetail shipping = new ShippingDetail();
        shipping.setAddress("Av. Lima 123");
        shipping.setDistrict(District.SAN_MIGUEL);
        shipping.setDescription("Frente al parque");
        shipping.setEstimatedDeliveryDate(LocalDate.of(2026, 7, 1));
        shipping.setActualDeliveryDate(null);

        Order order = order(1, quotation, OrderStatus.IN_PREPARATION);
        order.setItems(List.of(item));
        order.setShippingDetail(shipping);
        order.setFinalTotal(100.0);
        order.setPartialTotal(100.0);
        order.setTotalDiscount(0.0);

        var dto = service.toResponseDTO(order, 10);

        assertThat(dto.getId()).isEqualTo(1);
        assertThat(dto.getCustomer()).isEqualTo("Ana Lopez Diaz");
        assertThat(dto.getStatusLabel()).isEqualTo("En proceso");
        assertThat(dto.getStoreId()).isEqualTo(10);
        assertThat(dto.getItemsDetail()).hasSize(1);
        assertThat(dto.getItemsDetail().get(0).getSize()).isEqualTo("M");
        assertThat(dto.getItemsDetail().get(0).getColor()).isEqualTo("BLACK");
        assertThat(dto.getObservations()).isEqualTo("Enviar pronto");
        assertThat(dto.getShippingDetail()).isNotNull();
        assertThat(dto.getShippingDetail().getAddress()).isEqualTo("Av. Lima 123");
        assertThat(dto.getShippingDetail().getDistrict()).isEqualTo("SAN_MIGUEL");
    }

    @Test
    void toResponseDTOHandlesNullQuotationAndItems() {
        Order order = order(2, null, OrderStatus.CANCELLED);
        order.setItems(null);
        order.setShippingDetail(null);
        order.setFinalTotal(0.0);

        var dto = service.toResponseDTO(order, 10);

        assertThat(dto.getCustomer()).isEqualTo("Cliente");
        assertThat(dto.getItemsDetail()).isEmpty();
        assertThat(dto.getShippingDetail()).isNull();
        assertThat(dto.getObservations()).isNull();
        assertThat(dto.getStatusLabel()).isEqualTo("Cancelado");
    }

    @Test
    void toItemResponseDTOHandlesNullVariantAndProduct() {
        OrderItem item = new OrderItem();
        item.setProductVariant(null);
        item.setQuantity(1);
        item.setUnitPrice(10.0);
        item.setSubTotal(10.0);

        Order order = order(3, quotation(5), OrderStatus.DELIVERED);
        order.setItems(List.of(item));
        order.setFinalTotal(10.0);

        var dto = service.toResponseDTO(order, 10);

        assertThat(dto.getItemsDetail().get(0).getProductId()).isNull();
        assertThat(dto.getItemsDetail().get(0).getSize()).isNull();
        assertThat(dto.getItemsDetail().get(0).getColor()).isNull();
    }

    @Test
    void toItemResponseDTOHandlesVariantWithNullColor() {
        ProductVariant variant = new ProductVariant();
        variant.setId(1);
        variant.setSize("L");
        variant.setColor(null);
        variant.setStock(3);

        OrderItem item = new OrderItem();
        item.setProductVariant(variant);
        item.setQuantity(1);
        item.setUnitPrice(20.0);
        item.setSubTotal(20.0);

        Order order = order(4, quotation(5), OrderStatus.IN_TRANSIT);
        order.setItems(List.of(item));
        order.setFinalTotal(20.0);

        var dto = service.toResponseDTO(order, 10);

        assertThat(dto.getItemsDetail().get(0).getColor()).isNull();
        assertThat(dto.getStatusLabel()).isEqualTo("Enviado");
    }

    // =========================================================================
    // findByCustomerAndStore (null-safe ante datos históricos inconsistentes)
    // =========================================================================

    @Test
    void findByCustomerAndStoreReturnsOrdersMatchingStore() {
        pe.edu.pucp.kingstore.domain.model.store.Store store =
                new pe.edu.pucp.kingstore.domain.model.store.Store();
        store.setId(10);
        pe.edu.pucp.kingstore.domain.model.product.Product product =
                new pe.edu.pucp.kingstore.domain.model.product.Product();
        product.setStore(store);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);

        OrderItem item = new OrderItem();
        item.setProductVariant(variant);

        Order order = order(1, quotation(5), OrderStatus.PAYMENT_CONFIRMED);
        order.setItems(new ArrayList<>(List.of(item)));

        when(orderRepository.findByQuotation_ShoppingCart_Customer_Id(1))
                .thenReturn(List.of(order));

        assertThat(service.findByCustomerAndStore(1, 10)).containsExactly(order);
    }

    @Test
    void findByCustomerAndStoreSkipsOrdersWithNullProductStoreWithoutError() {
        // Dato histórico inconsistente: el producto no tiene tienda asociada.
        // No debe lanzar NullPointerException; simplemente se omite el pedido.
        pe.edu.pucp.kingstore.domain.model.product.Product product =
                new pe.edu.pucp.kingstore.domain.model.product.Product();
        product.setStore(null);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);

        OrderItem item = new OrderItem();
        item.setProductVariant(variant);

        Order order = order(1, quotation(5), OrderStatus.PAYMENT_CONFIRMED);
        order.setItems(new ArrayList<>(List.of(item)));

        when(orderRepository.findByQuotation_ShoppingCart_Customer_Id(1))
                .thenReturn(List.of(order));

        assertThat(service.findByCustomerAndStore(1, 10)).isEmpty();
    }

    @Test
    void findByCustomerAndStoreSkipsNullItemsAndFindsOrderInStore() {
        Order nullItemOrder = order(1, quotation(5), OrderStatus.PAYMENT_CONFIRMED);
        ArrayList<OrderItem> inconsistentItems = new ArrayList<>();
        inconsistentItems.add(null);
        nullItemOrder.setItems(inconsistentItems);

        pe.edu.pucp.kingstore.domain.model.store.Store store =
                new pe.edu.pucp.kingstore.domain.model.store.Store();
        store.setId(10);
        pe.edu.pucp.kingstore.domain.model.product.Product product =
                new pe.edu.pucp.kingstore.domain.model.product.Product();
        product.setStore(store);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        OrderItem item = new OrderItem();
        item.setProductVariant(variant);
        Order matching = order(2, quotation(6), OrderStatus.PAYMENT_CONFIRMED);
        matching.setItems(new ArrayList<>(List.of(item)));

        when(orderRepository.findByQuotation_ShoppingCart_Customer_Id(1))
                .thenReturn(List.of(nullItemOrder, matching));

        assertThat(service.findByCustomerInStore(2, 1, 10)).isSameAs(matching);
        assertThatThrownBy(() -> service.findByCustomerInStore(99, 1, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void toResponseDTOStatusLabelPaymentConfirmedAndDelivered() {
        Order confirmed = order(5, quotation(5), OrderStatus.PAYMENT_CONFIRMED);
        confirmed.setItems(null);
        confirmed.setFinalTotal(0.0);
        assertThat(service.toResponseDTO(confirmed, 10).getStatusLabel()).isEqualTo("Pagado");

        Order delivered = order(6, quotation(5), OrderStatus.DELIVERED);
        delivered.setItems(null);
        delivered.setFinalTotal(0.0);
        assertThat(service.toResponseDTO(delivered, 10).getStatusLabel()).isEqualTo("Entregado");
    }
    // =========================================================================
// createFromQuotation
// =========================================================================

    @Test
    void createFromQuotationCreatesOrderWithItemsCopied() {
        pe.edu.pucp.kingstore.domain.model.product.Product product =
                new pe.edu.pucp.kingstore.domain.model.product.Product();
        product.setId(1);

        pe.edu.pucp.kingstore.domain.model.product.ProductVariant variant =
                new pe.edu.pucp.kingstore.domain.model.product.ProductVariant();
        variant.setId(1);
        variant.setProduct(product);

        pe.edu.pucp.kingstore.domain.model.quotation.QuotationItem qi =
                new pe.edu.pucp.kingstore.domain.model.quotation.QuotationItem();
        qi.setProductVariant(variant);
        qi.setQuantity(2);
        qi.setPrice(100.0);
        qi.setSubTotal(200.0);

        pe.edu.pucp.kingstore.domain.model.quotation.Quotation quotation =
                new pe.edu.pucp.kingstore.domain.model.quotation.Quotation();
        quotation.setId(1);
        quotation.setStatus(pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus.APPROVED);
        quotation.setDiscount(0.0);
        quotation.setDesignFeeTotal(15.0);
        quotation.setDesignFeePercentageApplied(15.0);
        quotation.setItems(new java.util.ArrayList<>(List.of(qi)));

        when(quotationRepository.findById(1)).thenReturn(Optional.of(quotation));
        when(orderRepository.findByQuotationId(1)).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = service.createFromQuotation(quotation);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(result.getPartialTotal()).isEqualTo(200.0);
        assertThat(result.getFinalTotal()).isEqualTo(236.0);
        assertThat(result.getDesignFeeTotal()).isEqualTo(15.0);
        assertThat(result.getDesignFeePercentageApplied()).isEqualTo(15.0);
    }

    @Test
    void createFromQuotationKeepsDesignFeeSnapshotOnExistingOrder() {
        pe.edu.pucp.kingstore.domain.model.quotation.Quotation quotation =
                new pe.edu.pucp.kingstore.domain.model.quotation.Quotation();
        quotation.setId(1);
        quotation.setStatus(pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus.APPROVED);
        quotation.setItems(new java.util.ArrayList<>());

        Order existing = new Order();
        existing.setId(1);
        existing.setDesignFeeTotal(10.0);
        existing.setDesignFeePercentageApplied(10.0);
        when(quotationRepository.findById(1)).thenReturn(Optional.of(quotation));
        when(orderRepository.findByQuotationId(1)).thenReturn(Optional.of(existing));

        Order result = service.createFromQuotation(quotation);

        assertThat(result).isSameAs(existing);
        assertThat(result.getDesignFeeTotal()).isEqualTo(10.0);
        assertThat(result.getDesignFeePercentageApplied()).isEqualTo(10.0);
    }

    @Test
    void createFromQuotationThrowsWhenQuotationNotApproved() {
        pe.edu.pucp.kingstore.domain.model.quotation.Quotation quotation =
                new pe.edu.pucp.kingstore.domain.model.quotation.Quotation();
        quotation.setId(1);
        quotation.setStatus(pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus.PENDING);
        quotation.setItems(new java.util.ArrayList<>());

        when(quotationRepository.findById(1)).thenReturn(Optional.of(quotation));

        assertThatThrownBy(() -> service.createFromQuotation(quotation))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("approved quotation");
    }

    @Test
    void createFromQuotationReturnsExistingOrderWhenOrderAlreadyExists() {
        pe.edu.pucp.kingstore.domain.model.quotation.Quotation quotation =
                new pe.edu.pucp.kingstore.domain.model.quotation.Quotation();
        quotation.setId(1);
        quotation.setStatus(pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus.APPROVED);
        quotation.setItems(new java.util.ArrayList<>());

        Order existing = new Order();
        existing.setId(1);
        when(quotationRepository.findById(1)).thenReturn(Optional.of(quotation));
        when(orderRepository.findByQuotationId(1)).thenReturn(Optional.of(existing));

        assertThat(service.createFromQuotation(quotation)).isSameAs(existing);
    }

// =========================================================================
// advanceStatus
// =========================================================================

    @Test
    void advanceStatusFollowsCorrectFlow() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.advanceStatus(1).getStatus()).isEqualTo(OrderStatus.IN_PREPARATION);

        order.setStatus(OrderStatus.IN_PREPARATION);
        assertThat(service.advanceStatus(1).getStatus()).isEqualTo(OrderStatus.IN_TRANSIT);

        order.setStatus(OrderStatus.IN_TRANSIT);
        assertThat(service.advanceStatus(1).getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void advanceStatusThrowsWhenAlreadyDelivered() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.advanceStatus(1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be advanced");
    }

// =========================================================================
// cancel
// =========================================================================

    @Test
    void cancelChangesStatusToCancelledWithReason() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.IN_PREPARATION);
        Quotation quotation = quotation(5);
        order.setQuotation(quotation);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = service.cancel(1, "Cliente desistió");

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(quotation.getObservations()).isEqualTo("Cliente desistió");
    }

    @Test
    void cancelThrowsWhenReasonIsBlank() {
        assertThatThrownBy(() -> service.cancel(1, ""))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("reason is required");
    }

    @Test
    void cancelThrowsWhenOrderAlreadyDelivered() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancel(1, "motivo"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be cancelled");

        order.setStatus(OrderStatus.CANCELLED);
        assertThatThrownBy(() -> service.cancel(1, "motivo"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cannot be cancelled");
    }

// =========================================================================
// ship
// =========================================================================

    @Test
    void shipChangesStatusToInTransitAndSavesReference() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.IN_PREPARATION);
        ShippingDetail shipping = new ShippingDetail();
        order.setShippingDetail(shipping);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = service.ship(1, "GU-12345");

        assertThat(result.getStatus()).isEqualTo(OrderStatus.IN_TRANSIT);
        assertThat(shipping.getDescription()).isEqualTo("GU-12345");
    }

    @Test
    void shipStoresReferenceInQuotationObservationsWhenShippingDetailIsMissing() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.IN_PREPARATION);
        Quotation quotation = quotation(5);
        order.setQuotation(quotation);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = service.ship(1, "MOTO-9");

        assertThat(result.getStatus()).isEqualTo(OrderStatus.IN_TRANSIT);
        assertThat(quotation.getObservations()).isEqualTo("Shipping ref: MOTO-9");
    }

    @Test
    void shipThrowsWhenReferenceIsBlank() {
        assertThatThrownBy(() -> service.ship(1, ""))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("reference is required");
    }

    @Test
    void shipThrowsWhenOrderNotInPreparation() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.ship(1, "GU-12345"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("in preparation");
    }

// =========================================================================
// setShippingAddress
// =========================================================================

    private ShippingAddressRequestDTO shippingRequest(String address, String district, String reference) {
        ShippingAddressRequestDTO req = new ShippingAddressRequestDTO();
        req.setAddress(address);
        req.setDistrict(district);
        req.setReference(reference);
        return req;
    }

    @Test
    void setShippingAddressCreatesNewShippingDetail() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = service.setShippingAddress(1, shippingRequest("Av. Lima 123", "MIRAFLORES", "Frente al banco"));

        assertThat(result.getShippingDetail()).isNotNull();
        assertThat(result.getShippingDetail().getAddress()).isEqualTo("Av. Lima 123");
        assertThat(result.getShippingDetail().getDistrict()).isEqualTo(District.MIRAFLORES);
        assertThat(result.getShippingDetail().getDescription()).isEqualTo("Frente al banco");
    }

    @Test
    void setShippingAddressUpdatesExistingShippingDetail() {
        ShippingDetail existing = new ShippingDetail();
        existing.setAddress("old address");
        existing.setDistrict(District.CALLAO);
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
        order.setShippingDetail(existing);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = service.setShippingAddress(1, shippingRequest("Av. Lima 456", "SAN_ISIDRO", null));

        assertThat(result.getShippingDetail()).isSameAs(existing);
        assertThat(result.getShippingDetail().getAddress()).isEqualTo("Av. Lima 456");
        assertThat(result.getShippingDetail().getDistrict()).isEqualTo(District.SAN_ISIDRO);
        assertThat(result.getShippingDetail().getDescription()).isNull();
    }

    @Test
    void setShippingAddressThrowsWhenCancelled() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.setShippingAddress(1, shippingRequest("Av. Lima 123", "CALLAO", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cancelled");
    }

    @Test
    void setShippingAddressThrowsWhenDelivered() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.DELIVERED);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.setShippingAddress(1, shippingRequest("Av. Lima 123", "CALLAO", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("delivered");
    }

    @Test
    void setShippingAddressThrowsWhenAddressEmpty() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.setShippingAddress(1, shippingRequest("", "CALLAO", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("address is required");
    }

    @Test
    void setShippingAddressThrowsWhenDistrictInvalid() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.setShippingAddress(1, shippingRequest("Av. Lima 123", "INVALID_DISTRICT", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid district");
    }

    @Test
    void setShippingAddressAllowedOnPendingPayment() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = service.setShippingAddress(1, shippingRequest("Av. Lima 123", "CALLAO", null));
        assertThat(result.getShippingDetail()).isNotNull();
    }

    @Test
    void setShippingAddressSetsRecipientNameAndPhone() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShippingAddressRequestDTO req = shippingRequest("Av. Lima 123", "MIRAFLORES", "Portón azul");
        req.setRecipientName("Juan Pérez");
        req.setPhone("987654321");

        Order result = service.setShippingAddress(1, req);
        assertThat(result.getShippingDetail().getRecipientName()).isEqualTo("Juan Pérez");
        assertThat(result.getShippingDetail().getPhone()).isEqualTo("987654321");
    }

    @Test
    void setShippingAddressAcceptsCaseInsensitiveDistrict() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = service.setShippingAddress(1, shippingRequest("Av. Lima 123", "lince", null));
        assertThat(result.getShippingDetail().getDistrict()).isEqualTo(District.LINCE);
    }

    @Test
    void setShippingAddressAcceptsDistrictWithSpaces() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = service.setShippingAddress(1, shippingRequest("Av. Lima 123", "San Miguel", null));
        assertThat(result.getShippingDetail().getDistrict()).isEqualTo(District.SAN_MIGUEL);
    }

    @Test
    void setShippingAddressAcceptsLosOlivosEnum() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = service.setShippingAddress(1, shippingRequest("Jr. Hortencia 546", "LOS_OLIVOS", "Frente al parque"));
        assertThat(result.getShippingDetail().getDistrict()).isEqualTo(District.LOS_OLIVOS);
        assertThat(result.getShippingDetail().getAddress()).isEqualTo("Jr. Hortencia 546");
    }

    @Test
    void setShippingAddressAcceptsLosOlivosLabel() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = service.setShippingAddress(1, shippingRequest("Jr. Hortencia 546", "Los Olivos", "Frente al parque"));
        assertThat(result.getShippingDetail().getDistrict()).isEqualTo(District.LOS_OLIVOS);
    }

    @Test
    void setShippingAddressAcceptsLosOlivosLowercase() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order result = service.setShippingAddress(1, shippingRequest("Jr. Hortencia 546", "los olivos", "Frente al parque"));
        assertThat(result.getShippingDetail().getDistrict()).isEqualTo(District.LOS_OLIVOS);
    }

    @Test
    void setShippingAddressResponseIncludesShippingDetail() {
        Order order = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.PAYMENT_CONFIRMED);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ShippingAddressRequestDTO req = shippingRequest("Av. Arequipa 500", "SURCO", "Al lado del banco");
        req.setRecipientName("María López");
        req.setPhone("912345678");

        Order result = service.setShippingAddress(1, req);
        ShippingDetail detail = result.getShippingDetail();
        assertThat(detail).isNotNull();
        assertThat(detail.getAddress()).isEqualTo("Av. Arequipa 500");
        assertThat(detail.getDistrict()).isEqualTo(District.SURCO);
        assertThat(detail.getDescription()).isEqualTo("Al lado del banco");
        assertThat(detail.getRecipientName()).isEqualTo("María López");
        assertThat(detail.getPhone()).isEqualTo("912345678");
    }

}
