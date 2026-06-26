package pe.edu.pucp.kingstore.api.controller.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.kingstore.api.context.CustomerContext;
import pe.edu.pucp.kingstore.domain.dto.quotation.QuotationCreateRequestDTO;
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
                                    Authentication authentication,
                                    @RequestBody(required = false) QuotationCreateRequestDTO request) {
        try {
            Store store       = customerContext.store(slug);
            Customer customer = customerContext.customer(authentication, store);
            // getOrCreateCart garantiza un carrito activo SIN cotización (nunca uno ya
            // cotizado), por lo que aquí solo hay que crear la cotización y desactivar.
            ShoppingCart cart = shoppingCartService.getOrCreateCart(customer);

            String description = request != null ? request.getDescription() : null;
            Quotation quotation = quotationService.createFromCart(cart, description);
            // Desactivar el carrito SOLO tras crear la cotización con éxito, para que
            // el próximo GET /cart devuelva un carrito nuevo y vacío. Si createFromCart
            // lanza (carrito vacío o, en una carrera, ya cotizado), el carrito NO se
            // desactiva y no se pierde nada.
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
