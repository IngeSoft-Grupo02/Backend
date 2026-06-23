package pe.edu.pucp.kingstore.api.controller.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.kingstore.api.context.CustomerContext;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.service.cart.ShoppingCartService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.quotation.QuotationService;
import pe.edu.pucp.kingstore.service.order.OrderService;
import java.util.Map;

/**
 * Endpoints de cotizaciones para el cliente.
 *
 * Cliente 1:  Crear cotización desde el carrito.
 * Cliente 8:  Listar y ver detalle de cotizaciones.
 * Cliente 10: Aceptar o declinar una cotización respondida.
 */
@RestController
@RequestMapping("/stores/{slug}/quotations")
public class CustomerQuotationController {

    private final CustomerContext     customerContext;
    private final ShoppingCartService shoppingCartService;
    private final QuotationService    quotationService;
    private final OrderService orderService;

    public CustomerQuotationController(CustomerContext customerContext,
                                       ShoppingCartService shoppingCartService,
                                       QuotationService quotationService,
                                       OrderService orderService) {
        this.customerContext     = customerContext;
        this.shoppingCartService = shoppingCartService;
        this.quotationService    = quotationService;
        this.orderService        = orderService;
    }

    // POST /stores/{slug}/quotations
    @PostMapping
    public ResponseEntity<?> create(@PathVariable String slug,
                                    Authentication authentication) {
        try {
            Store store         = customerContext.store(slug);
            Customer customer   = customerContext.customer(authentication, store);
            ShoppingCart cart   = shoppingCartService.getOrCreateCart(customer);
            Quotation quotation = quotationService.createFromCart(cart);
            return ResponseEntity.status(201).body(
                    quotationService.toResponseDTO(quotation, store.getId()));
        } catch (ResourceNotFoundException | BusinessRuleException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /stores/{slug}/quotations
    @GetMapping
    public ResponseEntity<?> findAll(@PathVariable String slug,
                                     Authentication authentication) {
        try {
            Store store       = customerContext.store(slug);
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
            Store store       = customerContext.store(slug);
            Customer customer = customerContext.customer(authentication, store);
            Quotation quotation = quotationService.findByCustomerInStore(
                    id, customer.getId(), store.getId());
            return ResponseEntity.ok(
                    quotationService.toResponseDTO(quotation, store.getId()));
        } catch (ResourceNotFoundException | BusinessRuleException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH /stores/{slug}/quotations/{id}/accept
    @PatchMapping("/{id}/accept")
    public ResponseEntity<?> accept(@PathVariable String slug,
                                    @PathVariable Integer id,
                                    Authentication authentication) {
        try {
            Store store       = customerContext.store(slug);
            Customer customer = customerContext.customer(authentication, store);
            quotationService.findByCustomerInStore(id, customer.getId(), store.getId());
            Quotation updated = quotationService.acceptByCustomer(id);
            orderService.createFromQuotation(updated);
            return ResponseEntity.ok(quotationService.toResponseDTO(updated, store.getId()));
        } catch (ResourceNotFoundException | BusinessRuleException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PATCH /stores/{slug}/quotations/{id}/decline
    @PatchMapping("/{id}/decline")
    public ResponseEntity<?> decline(@PathVariable String slug,
                                     @PathVariable Integer id,
                                     Authentication authentication) {
        try {
            Store store       = customerContext.store(slug);
            Customer customer = customerContext.customer(authentication, store);
            quotationService.findByCustomerInStore(id, customer.getId(), store.getId());
            Quotation updated = quotationService.declineByCustomer(id);
            return ResponseEntity.ok(
                    quotationService.toResponseDTO(updated, store.getId()));
        } catch (ResourceNotFoundException | BusinessRuleException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}