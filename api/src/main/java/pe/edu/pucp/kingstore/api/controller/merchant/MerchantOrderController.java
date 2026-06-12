package pe.edu.pucp.kingstore.api.controller.merchant;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.kingstore.api.context.MerchantContext;
import pe.edu.pucp.kingstore.domain.dto.order.OrderStatusRequestDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.order.OrderService;

import static pe.edu.pucp.kingstore.service.user.util.MerchantStringUtil.parseOrderStatus;

@RestController
@RequestMapping("/merchant")
public class MerchantOrderController extends BaseMerchantController {

    private final OrderService orderService;

    public MerchantOrderController(MerchantContext merchantContext,
                                   OrderService orderService) {
        super(merchantContext);
        this.orderService = orderService;
    }

    @GetMapping("/orders")
    public ResponseEntity<?> orders(Authentication authentication,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) Integer storeId) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            var orders = status == null || status.isBlank()
                    ? orderService.findByStoreId(store.getId())
                    : orderService.findByStoreIdAndStatus(store.getId(), parseOrderStatus(status));
            return ResponseEntity.ok(orders.stream()
                    .map(o -> orderService.toResponseDTO(o, store.getId()))
                    .toList());
        });
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<?> updateOrderStatus(Authentication authentication,
                                               @PathVariable Integer id,
                                               @RequestParam(required = false) Integer storeId,
                                               @RequestBody OrderStatusRequestDTO request) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            if (request.getStatus() == null) {
                throw new BusinessRuleException("Order status is required");
            }
            var order = orderService.findInStore(id, store.getId());
            var updated = orderService.changeStatus(order.getId(), request.getStatus());
            return ResponseEntity.ok(orderService.toResponseDTO(updated, store.getId()));
        });
    }
}