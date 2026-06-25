package pe.edu.pucp.kingstore.api.controller.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.kingstore.api.context.CustomerContext;
import pe.edu.pucp.kingstore.domain.dto.payment.PaymentRequestDTO;
import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.order.OrderService;
import pe.edu.pucp.kingstore.service.payment.PaymentReceiptService;

import java.util.Map;

/**
 * Endpoints de pago simulado para el cliente.
 * Cliente 11: Realizar pago simulado de un pedido.
 */
@RestController
@RequestMapping("/stores/{slug}/orders")
public class CustomerPaymentController {

    private final CustomerContext       customerContext;
    private final OrderService          orderService;
    private final PaymentReceiptService paymentReceiptService;

    public CustomerPaymentController(CustomerContext customerContext,
                                     OrderService orderService,
                                     PaymentReceiptService paymentReceiptService) {
        this.customerContext       = customerContext;
        this.orderService          = orderService;
        this.paymentReceiptService = paymentReceiptService;
    }
    // POST /stores/{slug}/orders/{id}/payment
    @PostMapping("/{id}/payment")
    public ResponseEntity<?> pay(@PathVariable String slug,
                                 @PathVariable Integer id,
                                 Authentication authentication,
                                 @RequestBody(required = false) PaymentRequestDTO request) {
        try {
            // CAMBIO: receiptType es obligatorio — el cliente debe elegir boleta o factura
            if (request == null || request.getReceiptType() == null) {
                throw new BusinessRuleException("Receipt type is required (BOLETA or FACTURA)");
            }

            Store store       = customerContext.store(slug);
            Customer customer = customerContext.customer(authentication, store);
            Order order = orderService.findByCustomerInStore(id, customer.getId(), store.getId());

            String ruc        = request.getRuc();
            var method        = request.getPaymentMethod();
            String cardNumber = request.getCardNumber();
            String cardHolder = request.getCardHolder();
            String expiryDate = request.getExpiryDate();
            String cvv        = request.getCvv();
            // CAMBIO: receiptType viene del request, ya validado arriba
            var receiptType   = request.getReceiptType();


            var receipt = paymentReceiptService.simulatePayment(
                    order, ruc, method, cardNumber, cardHolder, expiryDate, cvv, receiptType);

            return ResponseEntity.status(201).body(Map.of(
                    "message",       "Pago confirmado exitosamente",
                    "receiptId",     receipt.getId(),
                    "orderId",       order.getId(),
                    "total",         receipt.getFinalTotal(),
                    "paymentStatus", "APPROVED",
                    "receiptType",   receipt.getReceiptType().name()
            ));
        } catch (ResourceNotFoundException | BusinessRuleException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}