package pe.edu.pucp.kingstore.service.order;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class OrderPaymentExpirationScheduler {

    private final OrderService orderService;

    public OrderPaymentExpirationScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelay = 15_000, initialDelay = 15_000)
    public void expirePendingPayments() {
        orderService.expirePendingPayments();
    }
}
