package pe.edu.pucp.kingstore.api.controller.customer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.kingstore.api.context.CustomerContext;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.domain.dto.order.ShippingAddressRequestDTO;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.order.OrderService;

import java.util.Map;

/**
 * Endpoints de pedidos para el cliente.
 *
 * Cliente 7: Historial de pedidos y detalle.
 */
@RestController
@RequestMapping("/stores/{slug}/orders")
public class CustomerOrderController {

    private static final Logger log = LoggerFactory.getLogger(CustomerOrderController.class);

    private final CustomerContext customerContext;
    private final OrderService    orderService;

    public CustomerOrderController(CustomerContext customerContext,
                                   OrderService orderService) {
        this.customerContext = customerContext;
        this.orderService    = orderService;
    }

    // GET /stores/{slug}/orders
    @GetMapping
    public ResponseEntity<?> findAll(@PathVariable String slug,
                                     Authentication authentication) {
        try {
            Store store       = customerContext.store(slug);
            Customer customer = customerContext.customer(authentication, store);
            return ResponseEntity.ok(
                    orderService.findByCustomerAndStore(customer.getId(), store.getId())
                            .stream()
                            .map(o -> orderService.toResponseDTO(o, store.getId()))
                            .toList());
        } catch (ResourceNotFoundException | BusinessRuleException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // GET /stores/{slug}/orders/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> findById(@PathVariable String slug,
                                      @PathVariable Integer id,
                                      Authentication authentication) {
        try {
            Store store       = customerContext.store(slug);
            Customer customer = customerContext.customer(authentication, store);
            var order = orderService.findByCustomerInStore(id, customer.getId(), store.getId());
            return ResponseEntity.ok(orderService.toResponseDTO(order, store.getId()));
        } catch (ResourceNotFoundException | BusinessRuleException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /stores/{slug}/orders/{id}/shipping-address
    @PutMapping("/{id}/shipping-address")
    public ResponseEntity<?> setShippingAddress(@PathVariable String slug,
                                                @PathVariable Integer id,
                                                Authentication authentication,
                                                @RequestBody ShippingAddressRequestDTO request) {
        try {
            Store store       = customerContext.store(slug);
            Customer customer = customerContext.customer(authentication, store);
            orderService.findByCustomerInStore(id, customer.getId(), store.getId());
            var updated = orderService.setShippingAddress(id, request);
            return ResponseEntity.ok(orderService.toResponseDTO(updated, store.getId()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (BusinessRuleException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (DataAccessException e) {
            String cause = e.getMostSpecificCause().getMessage();
            log.error("Persistence error saving shipping address for order {}: {}", id, cause, e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "No se pudo guardar la dirección de envío."));
        } catch (Exception e) {
            log.error("Unexpected error saving shipping address for order {}: {}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "No se pudo guardar la dirección de envío."));
        }
    }
}
