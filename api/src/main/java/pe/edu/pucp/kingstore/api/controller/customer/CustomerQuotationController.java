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

    public CustomerQuotationController(CustomerContext customerContext,
                                       ShoppingCartService shoppingCartService,
                                       QuotationService quotationService) {
        this.customerContext     = customerContext;
        this.shoppingCartService = shoppingCartService;
        this.quotationService    = quotationService;
    }

    // POST /stores/{slug}/quotations
    @PostMapping
    public ResponseEntity<?> create(@PathVariable String slug,
                                    Authentication authentication) {
        try {
            Store store       = customerContext.store(slug);
            Customer customer = customerContext.customer(authentication, store);
            ShoppingCart cart = shoppingCartService.getOrCreateCart(customer);

            // Si el carrito activo ya tiene cotización, desactivarlo para romper el ciclo:
            // de lo contrario createFromCart lanzaría y deactivate nunca se ejecutaría,
            // dejando al cliente atrapado en el mismo carrito en cada intento.
            if (quotationService.findByShoppingCart(cart.getId()).isPresent()) {
                shoppingCartService.deactivate(cart.getId());
                return ResponseEntity.badRequest().body(Map.of("error", "Cart already has a quotation"));
            }

            Quotation quotation = quotationService.createFromCart(cart);
            // Desactivar el carrito para que la próxima cotización use uno nuevo.
            shoppingCartService.deactivate(cart.getId());
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
}