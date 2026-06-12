package pe.edu.pucp.kingstore.api.controller.merchant;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.kingstore.api.context.MerchantContext;
import pe.edu.pucp.kingstore.domain.dto.quotation.QuotationResponseRequestDTO;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.service.quotation.QuotationService;

import static pe.edu.pucp.kingstore.service.user.util.MerchantStringUtil.parseQuotationStatus;

@RestController
@RequestMapping("/merchant")
public class MerchantQuotationController extends BaseMerchantController {

    private final QuotationService quotationService;

    public MerchantQuotationController(MerchantContext merchantContext,
                                       QuotationService quotationService) {
        super(merchantContext);
        this.quotationService = quotationService;
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
                    .map(q -> quotationService.toResponseDTO(q, store.getId()))
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
            var quotation = quotationService.findInStore(id, store.getId());
            var responded = quotationService.respond(
                    quotation.getId(),
                    request.getStatus(),
                    request.getObservations()
            );
            return ResponseEntity.ok(quotationService.toResponseDTO(responded, store.getId()));
        });
    }
}