package pe.edu.pucp.kingstore.service.order;

import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;

import java.time.Duration;
import java.time.LocalDateTime;

public final class OrderPaymentTimeoutPolicy {

    public static final Duration PAYMENT_TTL = Duration.ofMinutes(5);

    private OrderPaymentTimeoutPolicy() {
    }

    public static LocalDateTime expirationCutoff() {
        return LocalDateTime.now().minus(PAYMENT_TTL);
    }

    public static boolean isExpired(Order order) {
        return order != null
                && order.getStatus() == OrderStatus.PENDING_PAYMENT
                && order.getCreatedAt() != null
                && !order.getCreatedAt().plus(PAYMENT_TTL).isAfter(LocalDateTime.now());
    }
}
