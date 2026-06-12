package pe.edu.pucp.kingstore.service.store;


import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.dto.store.DashboardResponse;
import pe.edu.pucp.kingstore.domain.dto.store.RecentOrderResponse;
import pe.edu.pucp.kingstore.domain.model.product.enums.ProductStatus;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.repository.order.OrderRepository;
import pe.edu.pucp.kingstore.repository.product.ProductRepository;
import pe.edu.pucp.kingstore.repository.quotation.QuotationRepository;
import pe.edu.pucp.kingstore.service.user.util.MerchantCustomerUtil;

import java.util.Comparator;
import java.util.List;

@Service
public class DashboardService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final QuotationRepository quotationRepository;

    public DashboardService(ProductRepository productRepository,
                            OrderRepository orderRepository,
                            QuotationRepository quotationRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.quotationRepository = quotationRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse getDashboardData(Integer storeId) {
        var products = productRepository.findByStoreId(storeId);
        var orders = orderRepository.findByStoreId(storeId);
        var quotations = quotationRepository.findByStoreId(storeId);

        long pendingOrders = orders.stream()
                .filter(o -> switch (o.getStatus()) {
                    case PAYMENT_CONFIRMED, IN_PREPARATION, IN_TRANSIT -> true;
                    default -> false;
                }).count();

        long pendingQuotes = quotations.stream()
                .filter(q -> q.getStatus() == QuotationStatus.PENDING).count();

        long drafts = products.stream()
                .filter(p -> p.getStatus() == ProductStatus.DRAFT).count();

        List<RecentOrderResponse> recentOrders = orders.stream()
                .sorted(Comparator.comparing(
                        pe.edu.pucp.kingstore.domain.model.order.Order::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(o -> {
                    var quotation = o.getQuotation();
                    var customer = (quotation != null && quotation.getShoppingCart() != null)
                            ? quotation.getShoppingCart().getCustomer()
                            : null;

                    return new RecentOrderResponse(
                            o.getId(),
                            MerchantCustomerUtil.customerName(customer),
                            switch (o.getStatus()) {
                                case PAYMENT_CONFIRMED -> "Pagado";
                                case IN_PREPARATION    -> "En proceso";
                                case IN_TRANSIT        -> "Enviado";
                                case DELIVERED         -> "Entregado";
                                case CANCELLED         -> "Cancelado";
                            },
                            o.getFinalTotal(),
                            o.getCreatedAt(),
                            storeId
                    );
                })
                .toList();

        return new DashboardResponse(pendingOrders, pendingQuotes, drafts, recentOrders);
    }
}