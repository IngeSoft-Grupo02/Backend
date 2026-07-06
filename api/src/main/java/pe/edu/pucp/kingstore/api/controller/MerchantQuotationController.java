package pe.edu.pucp.kingstore.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pe.edu.pucp.kingstore.api.context.MerchantContext;
import pe.edu.pucp.kingstore.domain.dto.quotation.QuotationResponseRequestDTO;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.service.quotation.QuotationOrderWorkflowService;
import pe.edu.pucp.kingstore.service.quotation.QuotationService;

import static pe.edu.pucp.kingstore.service.user.util.MerchantStringUtil.parseQuotationStatus;

@RestController
@RequestMapping("/merchant")
public class MerchantQuotationController extends BaseMerchantController {

    private final QuotationService quotationService;
    private final QuotationOrderWorkflowService quotationOrderWorkflowService;

    public MerchantQuotationController(MerchantContext merchantContext,
                                       QuotationService quotationService,
                                       QuotationOrderWorkflowService quotationOrderWorkflowService) {
        super(merchantContext);
        this.quotationService = quotationService;
        this.quotationOrderWorkflowService = quotationOrderWorkflowService;
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

    @PatchMapping("/quotations/{id}/respond")
    public ResponseEntity<?> respondQuotation(Authentication authentication,
                                              @PathVariable Integer id,
                                              @RequestParam(required = false) Integer storeId,
                                              @RequestBody QuotationResponseRequestDTO request) {
        return handle(() -> {
            Store store = currentMerchantStore(authentication, storeId);
            var responded = quotationOrderWorkflowService.respondMerchantQuotation(
                    id,
                    store.getId(),
                    request.getStatus(),
                    request.getObservations(),
                    request.getDiscountAmount());
            return ResponseEntity.ok(quotationService.toResponseDTO(responded.getId(), store.getId()));
        });
    }
}
