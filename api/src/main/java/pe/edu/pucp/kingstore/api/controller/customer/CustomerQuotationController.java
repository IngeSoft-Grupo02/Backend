package pe.edu.pucp.kingstore.api.controller.customer;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import pe.edu.pucp.kingstore.api.context.CustomerContext;
import pe.edu.pucp.kingstore.domain.dto.quotation.QuotationCreateRequestDTO;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.service.cart.ShoppingCartService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.quotation.QuotationDesignService;
import pe.edu.pucp.kingstore.service.quotation.QuotationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Endpoints de cotizaciones para el cliente.
 *
 * Cliente 1: Crear cotizacion desde el carrito.
 * Cliente 8: Listar y ver detalle de cotizaciones.
 * Cliente 10: Aceptar o declinar una cotizacion respondida.
 */
@RestController
@RequestMapping("/stores/{slug}/quotations")
public class CustomerQuotationController {

    private final CustomerContext customerContext;
    private final ShoppingCartService shoppingCartService;
    private final QuotationService quotationService;
    private final QuotationDesignService quotationDesignService;

    public CustomerQuotationController(CustomerContext customerContext,
                                       ShoppingCartService shoppingCartService,
                                       QuotationService quotationService,
                                       QuotationDesignService quotationDesignService) {
        this.customerContext = customerContext;
        this.shoppingCartService = shoppingCartService;
        this.quotationService = quotationService;
        this.quotationDesignService = quotationDesignService;
    }

    // POST /stores/{slug}/quotations
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(@PathVariable String slug,
                                    Authentication authentication,
                                    @RequestBody(required = false) QuotationCreateRequestDTO request) {
        String description = request != null ? request.getDescription() : null;
        return createQuotation(slug, authentication, description, List.of());
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createMultipart(@PathVariable String slug,
                                             Authentication authentication,
                                             @RequestParam(value = "description", required = false) String description,
                                             @RequestPart(value = "designs", required = false) List<MultipartFile> designs,
                                             @RequestPart(value = "files", required = false) List<MultipartFile> files) {
        return createQuotation(slug, authentication, description, mergeFiles(designs, files));
    }

    private ResponseEntity<?> createQuotation(String slug,
                                              Authentication authentication,
                                              String description,
                                              List<MultipartFile> designs) {
        try {
            Store store = customerContext.store(slug);
            Customer customer = customerContext.customer(authentication, store);
            ShoppingCart cart = shoppingCartService.getOrCreateCart(customer);

            Quotation quotation = quotationService.createFromCart(cart, description);
            quotationDesignService.uploadDesigns(quotation, store.getSlug(), store.getId(), designs);

            shoppingCartService.deactivate(cart.getId());
            return ResponseEntity.status(201).body(
                    quotationService.toResponseDTO(quotation, store.getId()));
        } catch (ResourceNotFoundException | BusinessRuleException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private List<MultipartFile> mergeFiles(List<MultipartFile> designs, List<MultipartFile> files) {
        List<MultipartFile> merged = new ArrayList<>();
        if (designs != null) merged.addAll(designs);
        if (files != null) merged.addAll(files);
        return merged;
    }

    // GET /stores/{slug}/quotations
    @GetMapping
    public ResponseEntity<?> findAll(@PathVariable String slug,
                                     Authentication authentication) {
        try {
            Store store = customerContext.store(slug);
            Customer customer = customerContext.customer(authentication, store);
            return ResponseEntity.ok(
                    quotationService.findByCustomerAndStore(customer.getId(), store.getId())
                            .stream()
                            .map(q -> quotationService.toResponseDTO(q, store.getId()))
                            .toList());
        } catch (ResourceNotFoundException | BusinessRuleException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /stores/{slug}/quotations/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable String slug,
                                      @PathVariable Integer id,
                                      Authentication authentication) {
        try {
            Store store = customerContext.store(slug);
            Customer customer = customerContext.customer(authentication, store);
            Quotation quotation = quotationService.findByCustomerInStore(
                    id, customer.getId(), store.getId());
            return ResponseEntity.ok(
                    quotationService.toResponseDTO(quotation, store.getId()));
        } catch (ResourceNotFoundException | BusinessRuleException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }
}
