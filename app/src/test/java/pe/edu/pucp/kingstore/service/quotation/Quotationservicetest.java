package pe.edu.pucp.kingstore.service.quotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.QuotationItem;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.repository.quotation.QuotationRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Cubre lo que QuotationService agrega sobre AbstractCrudService:
 *  - findByShoppingCart / findByStatus / findByStoreId / findByStoreIdAndStatus
 *  - findInStore (encontrado y no encontrado)
 *  - respond (válido e inválido)
 *  - validateForSave / recalculateTotals (todos sus branches)
 *  - toResponseDTO (con y sin cliente, con y sin items)
 */
@ExtendWith(MockitoExtension.class)
class QuotationServiceTest {

    @Mock
    private QuotationRepository quotationRepository;

    private QuotationService service;

    @BeforeEach
    void setUp() {
        service = new QuotationService(quotationRepository);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ShoppingCart cart(int id) {
        ShoppingCart cart = new ShoppingCart();
        cart.setId(id);
        return cart;
    }

    private QuotationItem item(double price, int quantity) {
        QuotationItem item = new QuotationItem();
        item.setPrice(price);
        item.setQuantity(quantity);
        ProductVariant variant = new ProductVariant();
        variant.setSize("M");
        variant.setColor(Color.BLACK);
        item.setProductVariant(variant);
        return item;
    }

    private Quotation quotation(Integer id, ShoppingCart cart, List<QuotationItem> items, double discount) {
        Quotation quotation = new Quotation();
        quotation.setId(id);
        quotation.setShoppingCart(cart);
        quotation.setItems(items);
        quotation.setDiscount(discount);
        return quotation;
    }

    // =========================================================================
    // findByShoppingCart
    // =========================================================================

    @Test
    void findByShoppingCartReturnsQuotationWhenExists() {
        Quotation quotation = quotation(1, cart(5), List.of(), 0);
        when(quotationRepository.findByShoppingCartId(5)).thenReturn(Optional.of(quotation));

        assertThat(service.findByShoppingCart(5)).contains(quotation);
    }

    @Test
    void findByShoppingCartThrowsWhenIdInvalid() {
        assertThatThrownBy(() -> service.findByShoppingCart(0))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.findByShoppingCart(null))
                .isInstanceOf(BusinessRuleException.class);
    }

    // =========================================================================
    // findByStatus
    // =========================================================================

    @Test
    void findByStatusReturnsListWhenStatusValid() {
        Quotation quotation = quotation(1, cart(5), List.of(), 0);
        when(quotationRepository.findByStatus(QuotationStatus.PENDING)).thenReturn(List.of(quotation));

        assertThat(service.findByStatus(QuotationStatus.PENDING)).containsExactly(quotation);
    }

    @Test
    void findByStatusThrowsWhenStatusNull() {
        assertThatThrownBy(() -> service.findByStatus(null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("status is required");
    }

    // =========================================================================
    // findByStoreId / findByStoreIdAndStatus
    // =========================================================================

    @Test
    void findByStoreIdReturnsRepositoryResult() {
        Quotation quotation = quotation(1, cart(5), List.of(), 0);
        when(quotationRepository.findByStoreId(10)).thenReturn(List.of(quotation));

        assertThat(service.findByStoreId(10)).containsExactly(quotation);
    }

    @Test
    void findByStoreIdThrowsWhenIdInvalid() {
        assertThatThrownBy(() -> service.findByStoreId(0))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void findByStoreIdAndStatusReturnsRepositoryResult() {
        Quotation quotation = quotation(1, cart(5), List.of(), 0);
        when(quotationRepository.findByStoreIdAndStatus(10, QuotationStatus.APPROVED))
                .thenReturn(List.of(quotation));

        assertThat(service.findByStoreIdAndStatus(10, QuotationStatus.APPROVED))
                .containsExactly(quotation);
    }

    @Test
    void findByStoreIdAndStatusThrowsWhenStatusNull() {
        assertThatThrownBy(() -> service.findByStoreIdAndStatus(10, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("status is required");
    }

    // =========================================================================
    // findInStore
    // =========================================================================

    @Test
    void findInStoreReturnsQuotationWhenPresent() {
        Quotation quotation = quotation(7, cart(5), List.of(), 0);
        when(quotationRepository.findByStoreId(10)).thenReturn(List.of(quotation));

        assertThat(service.findInStore(7, 10)).isEqualTo(quotation);
    }

    @Test
    void findInStoreThrowsResourceNotFoundWhenAbsent() {
        Quotation quotation = quotation(7, cart(5), List.of(), 0);
        when(quotationRepository.findByStoreId(10)).thenReturn(List.of(quotation));

        assertThatThrownBy(() -> service.findInStore(99, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void findInStoreThrowsWhenIdsInvalid() {
        assertThatThrownBy(() -> service.findInStore(0, 10))
                .isInstanceOf(BusinessRuleException.class);
        assertThatThrownBy(() -> service.findInStore(7, 0))
                .isInstanceOf(BusinessRuleException.class);
    }

    // =========================================================================
    // respond
    // =========================================================================

    @Test
    void respondThrowsWhenStatusNullOrPending() {
        assertThatThrownBy(() -> service.respond(1, null, "obs"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("must approve or reject");
        assertThatThrownBy(() -> service.respond(1, QuotationStatus.PENDING, "obs"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void respondApprovesAndPersistsQuotation() {
        Quotation quotation = quotation(1, cart(5), List.of(), 0);
        quotation.setStatus(QuotationStatus.PENDING);
        when(quotationRepository.findById(1)).thenReturn(Optional.of(quotation));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Quotation result = service.respond(1, QuotationStatus.APPROVED, "Todo correcto");

        assertThat(result.getStatus()).isEqualTo(QuotationStatus.APPROVED);
        assertThat(result.getObservations()).isEqualTo("Todo correcto");
        assertThat(result.getResponseAt()).isNotNull();
    }

    @Test
    void respondThrowsResourceNotFoundWhenQuotationMissing() {
        when(quotationRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.respond(1, QuotationStatus.REJECTED, "No disponible"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // =========================================================================
    // validateForSave / recalculateTotals (a través de create)
    // =========================================================================

    @Test
    void createThrowsWhenShoppingCartMissing() {
        Quotation quotation = quotation(null, null, List.of(), 0);

        assertThatThrownBy(() -> service.create(quotation))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("shopping cart");
    }

    @Test
    void createThrowsWhenShoppingCartIdMissing() {
        Quotation quotation = quotation(null, new ShoppingCart(), List.of(), 0);

        assertThatThrownBy(() -> service.create(quotation))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("shopping cart");
    }

    @Test
    void createDefaultsStatusToPendingAndRecalculatesTotals() {
        QuotationItem item = item(10.0, 2);
        Quotation quotation = quotation(null, cart(5), List.of(item), 0);
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Quotation saved = service.create(quotation);

        assertThat(saved.getStatus()).isEqualTo(QuotationStatus.PENDING);
        assertThat(item.getSubTotal()).isEqualTo(20.0);
        assertThat(saved.getSubTotal()).isEqualTo(20.0);
        assertThat(saved.getTotalAmount()).isEqualTo(20.0);
    }

    @Test
    void recalculateTotalsThrowsWhenQuantityNotPositive() {
        QuotationItem item = item(10.0, 0);
        Quotation quotation = quotation(null, cart(5), List.of(item), 0);

        assertThatThrownBy(() -> service.create(quotation))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("quantity must be positive");
    }

    @Test
    void recalculateTotalsThrowsWhenPriceNegative() {
        QuotationItem item = item(-1.0, 1);
        Quotation quotation = quotation(null, cart(5), List.of(item), 0);

        assertThatThrownBy(() -> service.create(quotation))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("price cannot be negative");
    }

    @Test
    void recalculateTotalsThrowsWhenDiscountOutOfRange() {
        QuotationItem item = item(10.0, 1);
        Quotation negativeDiscount = quotation(null, cart(5), List.of(item), -1);
        Quotation tooHighDiscount = quotation(null, cart(5), List.of(item), 100);

        assertThatThrownBy(() -> service.create(negativeDiscount))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("discount must be between");
        assertThatThrownBy(() -> service.create(tooHighDiscount))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void recalculateTotalsHandlesNullItemsList() {
        Quotation quotation = quotation(null, cart(5), null, 0);
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Quotation saved = service.create(quotation);

        assertThat(saved.getSubTotal()).isEqualTo(0.0);
        assertThat(saved.getTotalAmount()).isEqualTo(0.0);
    }

    // =========================================================================
    // toResponseDTO
    // =========================================================================

    @Test
    void toResponseDTOMapsAllFieldsWithCustomerAndItems() {
        Customer customer = new Customer();
        customer.setFirstName("Ana");
        customer.setPaternalSurname("Lopez");
        customer.setMaternalSurname("Diaz");

        ShoppingCart cart = cart(5);
        cart.setCustomer(customer);

        QuotationItem item = item(15.0, 2);
        item.setSubTotal(30.0);

        Quotation quotation = quotation(1, cart, List.of(item), 5);
        quotation.setStatus(QuotationStatus.APPROVED);
        quotation.setSubTotal(30.0);
        quotation.setTotalAmount(25.0);

        var dto = service.toResponseDTO(quotation, 10);

        assertThat(dto.getId()).isEqualTo(1);
        assertThat(dto.getCustomer()).isEqualTo("Ana Lopez Diaz");
        assertThat(dto.getStatus()).isEqualTo(QuotationStatus.APPROVED);
        assertThat(dto.getStatusLabel()).isEqualTo("Aprobada");
        assertThat(dto.getStoreId()).isEqualTo(10);
        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getItems().get(0).getVariant()).isEqualTo("M / BLACK");
        assertThat(dto.getItems().get(0).getSubTotal()).isEqualTo(30.0);
    }

    @Test
    void toResponseDTOHandlesNullCustomerAndItemsAndAllStatusLabels() {
        Quotation pending = quotation(2, cart(5), null, 0);
        pending.setStatus(QuotationStatus.PENDING);
        var pendingDto = service.toResponseDTO(pending, 10);
        assertThat(pendingDto.getCustomer()).isEqualTo("Cliente");
        assertThat(pendingDto.getItems()).isEmpty();
        assertThat(pendingDto.getStatusLabel()).isEqualTo("Pendiente");

        Quotation rejected = quotation(3, cart(5), List.of(), 0);
        rejected.setStatus(QuotationStatus.REJECTED);
        var rejectedDto = service.toResponseDTO(rejected, 10);
        assertThat(rejectedDto.getStatusLabel()).isEqualTo("Rechazada");
    }

    @Test
    void toResponseDTOHandlesItemWithNullProductVariant() {
        QuotationItem item = new QuotationItem();
        item.setPrice(5.0);
        item.setQuantity(1);
        item.setSubTotal(5.0);
        item.setProductVariant(null);

        Quotation quotation = quotation(4, cart(5), List.of(item), 0);
        quotation.setStatus(QuotationStatus.PENDING);

        var dto = service.toResponseDTO(quotation, 10);

        assertThat(dto.getItems().get(0).getVariant()).isNull();
    }
}