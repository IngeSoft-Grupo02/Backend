package pe.edu.pucp.kingstore.service.product;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import pe.edu.pucp.kingstore.domain.model.quotation.QuotationItem;
import pe.edu.pucp.kingstore.repository.order.OrderRepository;
import pe.edu.pucp.kingstore.repository.product.ProductVariantRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class StockAvailabilityService {

    private final ProductVariantRepository productVariantRepository;
    private final OrderRepository orderRepository;

    public StockAvailabilityService(ProductVariantRepository productVariantRepository,
                                    OrderRepository orderRepository) {
        this.productVariantRepository = productVariantRepository;
        this.orderRepository = orderRepository;
    }

    public record StockSnapshot(int physicalStock, int reservedStock, int availableStock) {
        public int shortageFor(int requestedQuantity) {
            return Math.max(0, requestedQuantity - availableStock);
        }
    }

    @Transactional(readOnly = true)
    public StockSnapshot snapshot(Integer variantId) {
        return snapshot(variantId, null);
    }

    @Transactional(readOnly = true)
    public StockSnapshot snapshot(Integer variantId, Integer excludedOrderId) {
        if (variantId == null) {
            return new StockSnapshot(0, 0, 0);
        }
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant", variantId));
        int reserved = pendingReservation(variantId, excludedOrderId);
        int available = Math.max(0, variant.getStock() - reserved);
        return new StockSnapshot(variant.getStock(), reserved, available);
    }

    @Transactional
    public void assertCanReserveQuotationItems(List<QuotationItem> quotationItems) {
        Map<Integer, Integer> requestedByVariant = quotationItems == null
                ? Map.of()
                : quotationItems.stream()
                .filter(item -> item.getProductVariant() != null && item.getProductVariant().getId() != null)
                .collect(Collectors.toMap(
                        item -> item.getProductVariant().getId(),
                        QuotationItem::getQuantity,
                        Integer::sum,
                        LinkedHashMap::new));

        requestedByVariant.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> assertVariantCanReserve(entry.getKey(), entry.getValue()));
    }

    @Transactional
    public void consumeStockForPaidOrder(Order order) {
        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            throw new BusinessRuleException("Order must have items to consume stock");
        }

        Map<Integer, Integer> requestedByVariant = order.getItems().stream()
                .filter(item -> item.getProductVariant() != null && item.getProductVariant().getId() != null)
                .collect(Collectors.toMap(
                        item -> item.getProductVariant().getId(),
                        item -> item.getQuantity() == null ? 0 : item.getQuantity(),
                        Integer::sum,
                        LinkedHashMap::new));

        requestedByVariant.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> consumeVariantStock(entry.getKey(), entry.getValue()));
    }

    @Transactional
    public void assertCanSetPhysicalStock(Integer variantId, int nextPhysicalStock) {
        if (variantId == null) {
            return;
        }
        if (nextPhysicalStock < 0) {
            throw new BusinessRuleException("Product variant stock cannot be negative");
        }
        productVariantRepository.findWithProductByIdForUpdate(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant", variantId));
        int reserved = pendingReservation(variantId, null);
        if (nextPhysicalStock < reserved) {
            throw new BusinessRuleException(
                    "No se puede reducir el stock a " + nextPhysicalStock
                            + " unidades porque ya hay " + reserved
                            + " unidades reservadas en pedidos pendientes de pago.");
        }
    }

    private void assertVariantCanReserve(Integer variantId, int requestedQuantity) {
        if (requestedQuantity <= 0) {
            throw new BusinessRuleException("Requested stock quantity must be positive");
        }
        ProductVariant variant = productVariantRepository.findWithProductByIdForUpdate(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant", variantId));
        int reserved = pendingReservation(variantId, null);
        int available = Math.max(0, variant.getStock() - reserved);
        if (requestedQuantity > available) {
            throw new BusinessRuleException(stockMessage(variant, requestedQuantity, available));
        }
    }

    private void consumeVariantStock(Integer variantId, int requestedQuantity) {
        if (requestedQuantity <= 0) {
            throw new BusinessRuleException("Requested stock quantity must be positive");
        }
        ProductVariant variant = productVariantRepository.findWithProductByIdForUpdate(variantId)
                .orElseThrow(() -> new ResourceNotFoundException("Product variant", variantId));
        if (requestedQuantity > variant.getStock()) {
            throw new BusinessRuleException(stockMessage(variant, requestedQuantity, variant.getStock()));
        }
        variant.setStock(variant.getStock() - requestedQuantity);
        productVariantRepository.save(variant);
    }

    private int pendingReservation(Integer variantId, Integer excludedOrderId) {
        Long value = orderRepository.sumPendingPaymentQuantityByVariantId(variantId, excludedOrderId);
        return value == null ? 0 : value.intValue();
    }

    private String stockMessage(ProductVariant variant, int requestedQuantity, int available) {
        String productName = variant.getProduct() != null && variant.getProduct().getName() != null
                ? variant.getProduct().getName()
                : "Producto";
        String color = variant.getColor() != null ? variant.getColor().name() : "color no registrado";
        int shortage = Math.max(0, requestedQuantity - available);
        return "Stock insuficiente para " + productName
                + " (" + variant.getSize() + " / " + color + "). "
                + "Solicitado: " + requestedQuantity
                + ", disponible: " + available
                + ", faltan: " + shortage + " unidades.";
    }
}
