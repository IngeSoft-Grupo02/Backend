package pe.edu.pucp.kingstore.api.controller.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pe.edu.pucp.kingstore.api.context.CustomerContext;
import pe.edu.pucp.kingstore.domain.dto.cart.CartItemRequestDTO;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.service.cart.ShoppingCartService;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;
import pe.edu.pucp.kingstore.service.product.ProductVariantService;
import pe.edu.pucp.kingstore.service.store.StoreService;
import pe.edu.pucp.kingstore.service.user.CustomerService;
import pe.edu.pucp.kingstore.repository.user.CustomerRepository;
import pe.edu.pucp.kingstore.domain.dto.product.CustomDesignRequestDTO;
import java.util.Map;

/**
 * Endpoints del carrito de compras del cliente.
 * Todas las operaciones están acotadas a la tienda del slug
 * y al cliente autenticado — aislamiento multi-tenant.
 *
 * Cliente 1: Agregar productos al carrito y enviar cotización.
 * Cliente 2: Descuento por volumen aplicado automáticamente al agregar.
 */
@RestController
@RequestMapping("/stores/{slug}/cart")
public class CustomerCartController {

    //private final StoreService           storeService;
    private final CustomerContext customerContext;
    private final ProductVariantService  productVariantService;
    private final ShoppingCartService    shoppingCartService;

    public CustomerCartController(//StoreService storeService,
                                  CustomerContext customerContext,
                                  ProductVariantService productVariantService,
                                  ShoppingCartService shoppingCartService) {
       // this.storeService          = storeService;
        this.customerContext    = customerContext;
        this.productVariantService = productVariantService;
        this.shoppingCartService   = shoppingCartService;
    }

    // GET /stores/{slug}/cart
    @GetMapping
    public ResponseEntity<?> getCart(@PathVariable String slug,
                                     Authentication authentication) {
        try {
            Store store =  customerContext.store (slug);
            Customer customer = customerContext.customer(authentication, store);
            ShoppingCart cart = shoppingCartService.getOrCreateCart(customer);
            return ResponseEntity.ok(shoppingCartService.toResponseDTO(cart));
        } catch (ResourceNotFoundException | BusinessRuleException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // POST /stores/{slug}/cart/items
    @PostMapping("/items")
    public ResponseEntity<?> addItem(@PathVariable String slug,
                                     Authentication authentication,
                                     @RequestBody CartItemRequestDTO request) {
        try {
            if (request.getProductVariantId() == null) {
                throw new BusinessRuleException("Product variant is required");
            }
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                throw new BusinessRuleException("Quantity must be positive");
            }
            Store store = customerContext.store(slug);
            Customer customer = customerContext.customer(authentication, store);
            ProductVariant variant = productVariantService.getByIdWithProduct(request.getProductVariantId());
            ShoppingCart cart = shoppingCartService.getOrCreateCart(customer);
            ShoppingCart updated = shoppingCartService.addItem(
                    cart, variant, request.getQuantity(), store.getId(),
                    Boolean.TRUE.equals(request.getSeparateItem()),
                    request.getSelectedProductImageUrl());
            return ResponseEntity.ok(shoppingCartService.toResponseDTO(updated));
        } catch (ResourceNotFoundException | BusinessRuleException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // PUT /stores/{slug}/cart/items/{itemId}
    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateItem(@PathVariable String slug,
                                        @PathVariable Integer itemId,
                                        Authentication authentication,
                                        @RequestBody CartItemRequestDTO request) {
        try {
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                throw new BusinessRuleException("Quantity must be positive");
            }
            Store store = customerContext.store(slug);
            Customer customer = customerContext.customer(authentication, store);
            ShoppingCart cart = shoppingCartService.getOrCreateCart(customer);
            ShoppingCart updated = shoppingCartService.updateItem(
                    cart, itemId, request.getQuantity(), store.getId());
            return ResponseEntity.ok(shoppingCartService.toResponseDTO(updated));
        } catch (ResourceNotFoundException | BusinessRuleException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /stores/{slug}/cart/items/{itemId}
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> removeItem(@PathVariable String slug,
                                        @PathVariable Integer itemId,
                                        Authentication authentication) {
        try {
            Store store = customerContext.store(slug);
            Customer customer = customerContext.customer(authentication, store);
            ShoppingCart cart = shoppingCartService.getOrCreateCart(customer);
            ShoppingCart updated = shoppingCartService.removeItem(cart, itemId);
            return ResponseEntity.ok(shoppingCartService.toResponseDTO(updated));
        } catch (ResourceNotFoundException | BusinessRuleException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    // PATCH /stores/{slug}/cart/items/{itemId}/design
    @PatchMapping("/items/{itemId}/design")
    public ResponseEntity<?> addDesign(@PathVariable String slug,
                                       @PathVariable Integer itemId,
                                       Authentication authentication,
                                       @RequestBody CustomDesignRequestDTO request) {
        try {
            Store store = customerContext.store(slug);
            Customer customer = customerContext.customer(authentication, store);
            ShoppingCart cart = shoppingCartService.getOrCreateCart(customer);
            ShoppingCart updated = shoppingCartService.addDesignToItem(cart, itemId, request);
            return ResponseEntity.ok(shoppingCartService.toResponseDTO(updated));
        } catch (ResourceNotFoundException | BusinessRuleException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
