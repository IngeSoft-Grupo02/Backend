package pe.edu.pucp.kingstore.api.controller.merchant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import pe.edu.pucp.kingstore.api.context.MerchantContext;
import pe.edu.pucp.kingstore.api.controller.MerchantDiscountController;
import pe.edu.pucp.kingstore.domain.dto.product.DiscountRequestDTO;
import pe.edu.pucp.kingstore.domain.dto.product.DiscountResponseDTO;
import pe.edu.pucp.kingstore.domain.model.product.Discount;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.product.enums.VolumeType;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.service.product.DiscountService;
import pe.edu.pucp.kingstore.service.product.ProductService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Cubre todos los endpoints de MerchantDiscountController:
 *  - GET /merchant/discounts
 *  - POST /merchant/discounts (con y sin productId)
 *  - PUT /merchant/discounts/{id} (con y sin productId)
 *  - DELETE /merchant/discounts/{id}
 */
@ExtendWith(MockitoExtension.class)
class MerchantDiscountControllerTest {

    @Mock private MerchantContext merchantContext;
    @Mock private DiscountService discountService;
    @Mock private ProductService productService;

    private MerchantDiscountController controller;
    private Authentication authentication;
    private Store store;

    @BeforeEach
    void setUp() {
        controller = new MerchantDiscountController(merchantContext, discountService, productService);
        authentication = mock(Authentication.class);
        store = new Store();
        store.setId(10);
    }

    private DiscountResponseDTO responseDTO(int id) {
        return new DiscountResponseDTO(id, null, VolumeType.UNIT, 1, 5, 10.0,
                true, "Activa", store.getId(), null, "Promo", "Porcentaje", 10.0, 1, 0, "Todo el catalogo");
    }

    private Product product(int id) {
        Product product = new Product();
        product.setId(id);
        product.setStore(store);
        return product;
    }

    // =========================================================================
    // GET /merchant/discounts
    // =========================================================================

    @Test
    void discountsReturnsAllForStore() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        Discount discount = new Discount();
        discount.setId(1);
        when(discountService.findByStoreId(10)).thenReturn(List.of(discount));
        when(discountService.toResponseDTO(discount)).thenReturn(responseDTO(1));

        var result = controller.discounts(authentication, 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        List<DiscountResponseDTO> body = (List<DiscountResponseDTO>) result.getBody();
        assertThat(body).hasSize(1);
    }

    // =========================================================================
    // POST /merchant/discounts
    // =========================================================================

    @Test
    void createDiscountWithoutProductIdReturns201() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        DiscountRequestDTO request = new DiscountRequestDTO();
        request.setProductId(null);

        Discount created = new Discount();
        created.setId(1);
        when(discountService.createForStore(store, null, request)).thenReturn(created);
        when(discountService.toResponseDTO(created)).thenReturn(responseDTO(1));

        var result = controller.createDiscount(authentication, 10, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(productService, never()).findInStore(anyInt(), anyInt());
    }

    @Test
    void createDiscountWithProductIdResolvesProduct() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        DiscountRequestDTO request = new DiscountRequestDTO();
        request.setProductId(5);

        Product product = product(5);
        when(productService.findInStore(5, 10)).thenReturn(product);

        Discount created = new Discount();
        created.setId(2);
        when(discountService.createForStore(store, product, request)).thenReturn(created);
        when(discountService.toResponseDTO(created)).thenReturn(responseDTO(2));

        var result = controller.createDiscount(authentication, 10, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        verify(productService).findInStore(5, 10);
        verify(discountService).createForStore(store, product, request);
    }

    // =========================================================================
    // PUT /merchant/discounts/{id}
    // =========================================================================

    @Test
    void updateDiscountWithoutProductId() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        Discount existing = new Discount();
        existing.setId(1);
        when(discountService.findInStore(1, 10)).thenReturn(existing);

        DiscountRequestDTO request = new DiscountRequestDTO();
        request.setProductId(null);

        Discount updated = new Discount();
        updated.setId(1);
        when(discountService.updateForStore(existing, store, null, request)).thenReturn(updated);
        when(discountService.toResponseDTO(updated)).thenReturn(responseDTO(1));

        var result = controller.updateDiscount(authentication, 1, 10, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(productService, never()).findInStore(anyInt(), anyInt());
    }

    @Test
    void updateDiscountWithProductIdResolvesProduct() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        Discount existing = new Discount();
        existing.setId(1);
        when(discountService.findInStore(1, 10)).thenReturn(existing);

        DiscountRequestDTO request = new DiscountRequestDTO();
        request.setProductId(7);
        Product product = product(7);
        when(productService.findInStore(7, 10)).thenReturn(product);

        Discount updated = new Discount();
        updated.setId(1);
        when(discountService.updateForStore(existing, store, product, request)).thenReturn(updated);
        when(discountService.toResponseDTO(updated)).thenReturn(responseDTO(1));

        var result = controller.updateDiscount(authentication, 1, 10, request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(productService).findInStore(7, 10);
    }

    // =========================================================================
    // DELETE /merchant/discounts/{id}
    // =========================================================================

    @Test
    void deleteDiscountMarksDeletedAndReturnsMessage() {
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        Discount existing = new Discount();
        existing.setId(1);
        when(discountService.findInStore(1, 10)).thenReturn(existing);

        var result = controller.deleteDiscount(authentication, 1, 10);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) result.getBody();
        assertThat(body.get("message")).isEqualTo("Discount deleted successfully");
        verify(discountService).markDeleted(1);
    }
}
