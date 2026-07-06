package pe.edu.pucp.kingstore.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.kingstore.api.context.MerchantContext;
import pe.edu.pucp.kingstore.domain.dto.quotation.QuotationResponseRequestDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.service.quotation.QuotationService;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.service.order.OrderService;
import static pe.edu.pucp.kingstore.service.user.util.MerchantStringUtil.parseQuotationStatus;

@RestController
@RequestMapping("/merchant")
public class MerchantQuotationController extends BaseMerchantController {

    private final QuotationService quotationService;
    private final OrderService orderService;

    public MerchantQuotationController(MerchantContext merchantContext,
                                       QuotationService quotationService,
                                       OrderService orderService) {
        super(merchantContext);
        this.quotationService = quotationService;
        this.orderService = orderService;
    }

    @GetMapping("/quotations")
    public ResponseEntity<?> quotations(Authentication authentication,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) Integer storeId) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            var quotations = status == null || status.isBlank()
                    ? quotationService.findByStoreId(store.getId())
                    : quotationService.findByStoreIdAndStatus(store.getId(), parseQuotationStatus(status));
            return ResponseEntity.ok(quotations.stream()
                    .map(q -> quotationService.toResponseDTO(q.getId(), store.getId()))
                    .toList());
        });
    }

    // DESPUÉS de respond(), agrega la creación de orden si status es APPROVED:
    @PatchMapping("/quotations/{id}/respond")
    public ResponseEntity<?> respondQuotation(Authentication authentication,
                                              @PathVariable Integer id,
                                              @RequestParam(required = false) Integer storeId,
                                              @RequestBody QuotationResponseRequestDTO request) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            var quotation = quotationService.findInStore(id, store.getId());
            var responded = quotationService.respond(
                    quotation.getId(),
                    request.getStatus(),
                    request.getObservations(),
                    request.getDiscountAmount()
            );
            // Merchant aprueba → orden creada automáticamente
            if (responded.getStatus() == QuotationStatus.APPROVED) {
                orderService.createFromQuotation(responded.getId());
            }
            return ResponseEntity.ok(quotationService.toResponseDTO(responded.getId(), store.getId()));
        });
    }
}
