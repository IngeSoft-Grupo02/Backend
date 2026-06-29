package pe.edu.pucp.kingstore.api.controller.merchant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import pe.edu.pucp.kingstore.api.context.MerchantContext;
import pe.edu.pucp.kingstore.api.controller.MerchantOrderController;
import pe.edu.pucp.kingstore.domain.dto.order.OrderCancelRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.order.OrderShipRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.order.OrderStatusRequestDTO;
import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.order.OrderService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MerchantOrderControllerTest {
    @Mock private MerchantContext merchantContext;
    @Mock private OrderService    orderService;

    private MerchantOrderController controller;
    private Authentication authentication;
    private Store store;
    private Order order;

    @BeforeEach
    void setUp() {
        controller     = new MerchantOrderController(merchantContext, orderService);
        authentication = mock(Authentication.class);
        store          = new Store();
        store.setId(10);
        order          = new Order();
        order.setId(1);
        order.setStatus(OrderStatus.IN_PREPARATION);
    }

    // ── GET /merchant/orders ─────────────────────────────────────────────────

    @Test
    void ordersWithoutStatusReturnsAllStoreOrders() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        when(orderService.findByStoreId(10)).thenReturn(List.of(order));
        when(orderService.toResponseDTO(eq(order), eq(10))).thenReturn(new pe.edu.pucp.kingstore.domain.dto.order.OrderResponseDTO());

        var result = controller.orders(authentication, null, 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<pe.edu.pucp.kingstore.domain.dto.order.OrderResponseDTO> body =
                (List<pe.edu.pucp.kingstore.domain.dto.order.OrderResponseDTO>) result.getBody();
        assertThat(body).hasSize(1);
    }

    @Test
    void ordersWithStatusUsesStatusFilter() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        when(orderService.findByStoreIdAndStatus(10, OrderStatus.IN_PREPARATION)).thenReturn(List.of(order));
        when(orderService.toResponseDTO(eq(order), eq(10))).thenReturn(new pe.edu.pucp.kingstore.domain.dto.order.OrderResponseDTO());

        var result = controller.orders(authentication, "in_preparation", 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // ── PATCH /merchant/orders/{id}/status ───────────────────────────────────

    @Test
    void updateOrderStatusReturnsUpdatedOrder() {
        OrderStatusRequestDTO request = new OrderStatusRequestDTO();
        request.setStatus(OrderStatus.IN_TRANSIT);
        order.setStatus(OrderStatus.IN_TRANSIT);

        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        when(orderService.findInStore(1, 10)).thenReturn(order);
        when(orderService.changeStatus(1, OrderStatus.IN_TRANSIT)).thenReturn(order);
        when(orderService.toResponseDTO(eq(order), eq(10))).thenReturn(new pe.edu.pucp.kingstore.domain.dto.order.OrderResponseDTO());

        var result = controller.updateOrderStatus(authentication, 1, 10, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void updateOrderStatusRequiresStatus() {
        OrderStatusRequestDTO request = new OrderStatusRequestDTO();
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);

        var result = controller.updateOrderStatus(authentication, 1, 10, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── PATCH /merchant/orders/{id}/advance ───────────────────────────────────

    @Test
    void advanceStatusReturnsUpdatedOrder() {
        order.setStatus(OrderStatus.IN_TRANSIT);
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        when(orderService.findInStore(1, 10)).thenReturn(order);
        when(orderService.advanceStatus(1)).thenReturn(order);
        when(orderService.toResponseDTO(eq(order), eq(10))).thenReturn(new pe.edu.pucp.kingstore.domain.dto.order.OrderResponseDTO());

        var result = controller.advanceStatus(authentication, 1, 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void advanceStatusReturnsBadRequestWhenAlreadyDelivered() {
        order.setStatus(OrderStatus.DELIVERED);
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        when(orderService.findInStore(1, 10)).thenReturn(order);
        when(orderService.advanceStatus(1))
                .thenThrow(new BusinessRuleException("Order cannot be advanced from status: DELIVERED"));

        var result = controller.advanceStatus(authentication, 1, 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── PATCH /merchant/orders/{id}/cancel ────────────────────────────────────

    @Test
    void cancelReturnsUpdatedOrder() {
        order.setStatus(OrderStatus.CANCELLED);
        OrderCancelRequestDTO request = new OrderCancelRequestDTO();
        request.setReason("Cliente desistió");

        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        when(orderService.findInStore(1, 10)).thenReturn(order);
        when(orderService.cancel(1, "Cliente desistió")).thenReturn(order);
        when(orderService.toResponseDTO(eq(order), eq(10))).thenReturn(new pe.edu.pucp.kingstore.domain.dto.order.OrderResponseDTO());

        var result = controller.cancel(authentication, 1, 10, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void cancelReturnsBadRequestWhenReasonBlank() {
        OrderCancelRequestDTO request = new OrderCancelRequestDTO();
        request.setReason("");

        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        when(orderService.findInStore(1, 10)).thenReturn(order);
        when(orderService.cancel(1, ""))
                .thenThrow(new BusinessRuleException("Cancellation reason is required"));

        var result = controller.cancel(authentication, 1, 10, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // ── PATCH /merchant/orders/{id}/ship ──────────────────────────────────────

    @Test
    void shipReturnsUpdatedOrder() {
        order.setStatus(OrderStatus.IN_TRANSIT);
        OrderShipRequestDTO request = new OrderShipRequestDTO();
        request.setShippingReference("GU-12345");

        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        when(orderService.findInStore(1, 10)).thenReturn(order);
        when(orderService.ship(1, "GU-12345")).thenReturn(order);
        when(orderService.toResponseDTO(eq(order), eq(10))).thenReturn(new pe.edu.pucp.kingstore.domain.dto.order.OrderResponseDTO());

        var result = controller.ship(authentication, 1, 10, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void shipReturnsBadRequestWhenReferenceBlank() {
        OrderShipRequestDTO request = new OrderShipRequestDTO();
        request.setShippingReference("");

        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        when(orderService.findInStore(1, 10)).thenReturn(order);
        when(orderService.ship(1, ""))
                .thenThrow(new BusinessRuleException("Shipping reference is required"));

        var result = controller.ship(authentication, 1, 10, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
