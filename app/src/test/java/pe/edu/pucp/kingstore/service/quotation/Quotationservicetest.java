package pe.edu.pucp.kingstore.service.quotation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.domain.model.product.CustomDesign;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.quotation.QuotationDesign;
import pe.edu.pucp.kingstore.domain.model.quotation.QuotationItem;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.repository.quotation.QuotationRepository;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
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
    void approveOrRejectQuotation_shouldNotOverwriteCustomerDescriptionWithObservations() {
        Quotation quotation = quotation(1, cart(5), List.of(), 0);
        quotation.setStatus(QuotationStatus.PENDING);
        quotation.setDescription("Necesito polos para evento corporativo");
        when(quotationRepository.findById(1)).thenReturn(Optional.of(quotation));
        when(quotationRepository.save(any(Quotation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Quotation result = service.respond(1, QuotationStatus.APPROVED, "Aprobado para entrega el viernes");

        assertThat(result.getDescription()).isEqualTo("Necesito polos para evento corporativo");
        assertThat(result.getObservations()).isEqualTo("Aprobado para entrega el viernes");
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
        item.setId(15);
        item.setSubTotal(30.0);
        item.setCustomerDescription("Bordado al frente");
        Product product = new Product();
        product.setId(20);
        product.setName("Polo Classic White");
        product.setBasePrice(15.0);
        product.setImageUrls(List.of(" ", "https://bucket.s3.amazonaws.com/products/polo-white.jpeg"));
        item.getProductVariant().setProduct(product);

        Quotation quotation = quotation(1, cart, List.of(item), 5);
        quotation.setStatus(QuotationStatus.APPROVED);
        quotation.setSubTotal(30.0);
        quotation.setTotalAmount(25.0);
        quotation.setDescription("Necesito polos para evento corporativo");
        quotation.setObservations("Aprobado con stock disponible");
        QuotationDesign generalDesign = new QuotationDesign();
        generalDesign.setId(7);
        generalDesign.setOriginalFileName("brief-general.pdf");
        generalDesign.setFileUrl("https://bucket.s3.amazonaws.com/design/street-kings/quotations/1/brief-general.pdf");
        generalDesign.setContentType("application/pdf");
        generalDesign.setSizeBytes(123456L);
        QuotationDesign itemDesign = new QuotationDesign();
        itemDesign.setId(8);
        itemDesign.setOriginalFileName("diseno-frontal.png");
        itemDesign.setFileUrl("https://bucket.s3.amazonaws.com/design/street-kings/quotations/1/diseno-frontal.png");
        itemDesign.setContentType("image/png");
        itemDesign.setSizeBytes(654321L);
        itemDesign.setQuotationItem(item);
        quotation.setDesigns(List.of(generalDesign, itemDesign));

        var dto = service.toResponseDTO(quotation, 10);

        assertThat(dto.getId()).isEqualTo(1);
        assertThat(dto.getCustomer()).isEqualTo("Ana Lopez Diaz");
        assertThat(dto.getStatus()).isEqualTo(QuotationStatus.APPROVED);
        assertThat(dto.getStatusLabel()).isEqualTo("Aprobada");
        assertThat(dto.getStoreId()).isEqualTo(10);
        assertThat(dto.getDescription()).isEqualTo("Necesito polos para evento corporativo");
        assertThat(dto.getObservations()).isEqualTo("Aprobado con stock disponible");
        assertThat(dto.getDesigns()).hasSize(1);
        assertThat(dto.getDesigns().get(0).getFileName()).isEqualTo("brief-general.pdf");
        assertThat(dto.getDesigns().get(0).getQuotationItemId()).isNull();
        assertThat(dto.getItems()).hasSize(1);
        assertThat(dto.getItems().get(0).getProductName()).isEqualTo("Polo Classic White");
        assertThat(dto.getItems().get(0).getProductImageUrl()).isEqualTo("https://bucket.s3.amazonaws.com/products/polo-white.jpeg");
        assertThat(dto.getItems().get(0).getVariant()).isEqualTo("M / BLACK");
        assertThat(dto.getItems().get(0).getSubTotal()).isEqualTo(30.0);
        assertThat(dto.getItems().get(0).getCustomerDescription()).isEqualTo("Bordado al frente");
        assertThat(dto.getItems().get(0).getDesigns()).hasSize(1);
        assertThat(dto.getItems().get(0).getDesigns().get(0).getFileName()).isEqualTo("diseno-frontal.png");
        assertThat(dto.getItems().get(0).getDesigns().get(0).getQuotationItemId()).isEqualTo(15);
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
    // =========================================================================
// createFromCart
// =========================================================================

    @Test
    void createFromCartCreatesQuotationWithItemsCopiedFromCart() {
        pe.edu.pucp.kingstore.domain.model.product.Product product =
                new pe.edu.pucp.kingstore.domain.model.product.Product();
        product.setId(1);
        product.setBasePrice(100.0);

        ProductVariant variant = new ProductVariant();
        variant.setId(1);
        variant.setProduct(product);

        pe.edu.pucp.kingstore.domain.model.cart.CartItem cartItem =
                new pe.edu.pucp.kingstore.domain.model.cart.CartItem();
        cartItem.setId(1);
        cartItem.setProductVariant(variant);
        cartItem.setQuantity(2);
        cartItem.setPrice(100.0);
        cartItem.setSubtotal(200.0);

        ShoppingCart cart = cart(1);
        cart.setItems(new java.util.ArrayList<>(List.of(cartItem)));
        cart.setDiscount(0.0);

        when(quotationRepository.findByShoppingCartId(1)).thenReturn(Optional.empty());
        when(quotationRepository.save(any(Quotation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Quotation result = service.createFromCart(cart);

        assertThat(result.getStatus()).isEqualTo(QuotationStatus.PENDING);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void createFromCartCopiesItemCustomerDescription() {
        pe.edu.pucp.kingstore.domain.model.product.Product product =
                new pe.edu.pucp.kingstore.domain.model.product.Product();
        product.setId(1);

        ProductVariant variant = new ProductVariant();
        variant.setId(1);
        variant.setProduct(product);

        CustomDesign customDesign = new CustomDesign();
        customDesign.setDescription("  Bordado izquierdo  ");

        pe.edu.pucp.kingstore.domain.model.cart.CartItem cartItem =
                new pe.edu.pucp.kingstore.domain.model.cart.CartItem();
        cartItem.setId(1);
        cartItem.setProductVariant(variant);
        cartItem.setQuantity(2);
        cartItem.setPrice(100.0);
        cartItem.setSubtotal(200.0);
        cartItem.setCustomDesign(customDesign);

        ShoppingCart cart = cart(1);
        cart.setItems(new java.util.ArrayList<>(List.of(cartItem)));
        cart.setDiscount(0.0);

        when(quotationRepository.findByShoppingCartId(1)).thenReturn(Optional.empty());
        when(quotationRepository.save(any(Quotation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Quotation result = service.createFromCart(cart);

        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().get(0).getCustomerDescription()).isEqualTo("Bordado izquierdo");
    }

    @Test
    void applyItemDesignFeesAddsFeeOnlyToItemsWithAssociatedDesigns() {
        pe.edu.pucp.kingstore.domain.model.product.Product designedProduct =
                new pe.edu.pucp.kingstore.domain.model.product.Product();
        designedProduct.setBasePrice(100.0);
        ProductVariant designedVariant = new ProductVariant();
        designedVariant.setId(101);
        designedVariant.setProduct(designedProduct);
        QuotationItem designedItem = new QuotationItem();
        designedItem.setId(201);
        designedItem.setProductVariant(designedVariant);
        designedItem.setQuantity(2);
        designedItem.setPrice(100.0);
        designedItem.setSubTotal(200.0);

        pe.edu.pucp.kingstore.domain.model.product.Product plainProduct =
                new pe.edu.pucp.kingstore.domain.model.product.Product();
        plainProduct.setBasePrice(50.0);
        ProductVariant plainVariant = new ProductVariant();
        plainVariant.setId(102);
        plainVariant.setProduct(plainProduct);
        QuotationItem plainItem = new QuotationItem();
        plainItem.setId(202);
        plainItem.setProductVariant(plainVariant);
        plainItem.setQuantity(2);
        plainItem.setPrice(50.0);
        plainItem.setSubTotal(100.0);

        QuotationDesign itemDesign = new QuotationDesign();
        itemDesign.setQuotationItem(designedItem);
        itemDesign.setActive(true);
        QuotationDesign generalDesign = new QuotationDesign();
        generalDesign.setQuotationItem(null);
        generalDesign.setActive(true);

        Quotation quotation = quotation(1, cart(1), new ArrayList<>(List.of(designedItem, plainItem)), 0);
        quotation.setDesigns(new ArrayList<>(List.of(itemDesign, generalDesign)));
        when(quotationRepository.save(any(Quotation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Quotation result = service.applyItemDesignFees(quotation);

        assertThat(result.getItems().get(0).getSubTotal()).isEqualTo(220.0);
        assertThat(result.getItems().get(1).getSubTotal()).isEqualTo(100.0);
        assertThat(result.getTotalAmount()).isEqualTo(320.0);
    }

    @Test
    void toResponseDTOExposesPricingBreakdownForDiscountAndDesign() {
        pe.edu.pucp.kingstore.domain.model.product.Product product =
                new pe.edu.pucp.kingstore.domain.model.product.Product();
        product.setId(1);
        product.setName("Polo");
        product.setBasePrice(100.0);
        ProductVariant variant = new ProductVariant();
        variant.setId(101);
        variant.setProduct(product);

        QuotationItem item = new QuotationItem();
        item.setId(201);
        item.setProductVariant(variant);
        item.setQuantity(10);
        item.setPrice(110.0);
        item.setSubTotal(1100.0);

        QuotationDesign design = new QuotationDesign();
        design.setId(301);
        design.setQuotationItem(item);
        design.setOriginalFileName("logo.png");
        design.setFileUrl("https://cdn.test/logo.png");
        design.setContentType("image/png");
        design.setSizeBytes(100L);
        design.setActive(true);

        Quotation quotation = quotation(1, cart(1), List.of(item), 0);
        quotation.setStatus(QuotationStatus.PENDING);
        quotation.setSubTotal(1100.0);
        quotation.setTotalAmount(1100.0);
        quotation.setDesigns(List.of(design));

        var dto = service.toResponseDTO(quotation, 10);

        assertThat(dto.getProductSubtotal()).isEqualTo(1000.0);
        assertThat(dto.getDesignFeeTotal()).isEqualTo(100.0);
        assertThat(dto.getItems().get(0).getDesignFeeAmount()).isEqualTo(100.0);
    }

    @Test
    void createQuotation_withDescription_shouldPersistDescription() {
        pe.edu.pucp.kingstore.domain.model.product.Product product =
                new pe.edu.pucp.kingstore.domain.model.product.Product();
        product.setId(1);

        ProductVariant variant = new ProductVariant();
        variant.setId(1);
        variant.setProduct(product);

        pe.edu.pucp.kingstore.domain.model.cart.CartItem cartItem =
                new pe.edu.pucp.kingstore.domain.model.cart.CartItem();
        cartItem.setId(1);
        cartItem.setProductVariant(variant);
        cartItem.setQuantity(2);
        cartItem.setPrice(100.0);
        cartItem.setSubtotal(200.0);

        ShoppingCart cart = cart(1);
        cart.setItems(new java.util.ArrayList<>(List.of(cartItem)));
        cart.setDiscount(0.0);

        when(quotationRepository.findByShoppingCartId(1)).thenReturn(Optional.empty());
        when(quotationRepository.save(any(Quotation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Quotation result = service.createFromCart(cart, "  Necesito polos para evento corporativo  ");

        assertThat(result.getDescription()).isEqualTo("Necesito polos para evento corporativo");
    }

    @Test
    void createQuotation_withoutDescription_shouldStillWork() {
        pe.edu.pucp.kingstore.domain.model.product.Product product =
                new pe.edu.pucp.kingstore.domain.model.product.Product();
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);

        pe.edu.pucp.kingstore.domain.model.cart.CartItem cartItem =
                new pe.edu.pucp.kingstore.domain.model.cart.CartItem();
        cartItem.setProductVariant(variant);
        cartItem.setQuantity(1);
        cartItem.setPrice(50.0);
        cartItem.setSubtotal(50.0);

        ShoppingCart cart = cart(1);
        cart.setItems(new java.util.ArrayList<>(List.of(cartItem)));
        cart.setDiscount(0.0);

        when(quotationRepository.findByShoppingCartId(1)).thenReturn(Optional.empty());
        when(quotationRepository.save(any(Quotation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Quotation result = service.createFromCart(cart, "   ");

        assertThat(result.getDescription()).isNull();
        assertThat(result.getStatus()).isEqualTo(QuotationStatus.PENDING);
    }

    @Test
    void createFromCartThrowsWhenCartIsEmpty() {
        ShoppingCart cart = cart(1);
        cart.setItems(new java.util.ArrayList<>());

        assertThatThrownBy(() -> service.createFromCart(cart))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at least one item");
    }

    @Test
    void createFromCartThrowsWhenAnyQuotationExistsForCart() {
        pe.edu.pucp.kingstore.domain.model.product.Product product =
                new pe.edu.pucp.kingstore.domain.model.product.Product();
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);

        pe.edu.pucp.kingstore.domain.model.cart.CartItem cartItem =
                new pe.edu.pucp.kingstore.domain.model.cart.CartItem();
        cartItem.setProductVariant(variant);
        cartItem.setQuantity(1);
        cartItem.setPrice(50.0);
        cartItem.setSubtotal(50.0);

        ShoppingCart cart = cart(1);
        cart.setItems(new java.util.ArrayList<>(List.of(cartItem)));
        cart.setDiscount(0.0);

        Quotation existing = quotation(1, cart, List.of(), 0);
        existing.setStatus(QuotationStatus.PENDING);
        when(quotationRepository.findByShoppingCartId(1)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.createFromCart(cart))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already has a quotation");
    }

// =========================================================================
// acceptByCustomer / declineByCustomer
// =========================================================================

    @Test
    void acceptByCustomerChangesStatusToApproved() {
        Quotation quotation = quotation(1, cart(1), List.of(), 0);
        quotation.setStatus(QuotationStatus.PENDING);

        when(quotationRepository.findById(1)).thenReturn(Optional.of(quotation));
        when(quotationRepository.save(any(Quotation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Quotation result = service.acceptByCustomer(1);

        assertThat(result.getStatus()).isEqualTo(QuotationStatus.APPROVED);
        assertThat(result.getResponseAt()).isNotNull();
    }

    @Test
    void acceptByCustomerThrowsWhenNotPending() {
        Quotation quotation = quotation(1, cart(1), List.of(), 0);
        quotation.setStatus(QuotationStatus.APPROVED);

        when(quotationRepository.findById(1)).thenReturn(Optional.of(quotation));

        assertThatThrownBy(() -> service.acceptByCustomer(1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pending quotations");
    }

    @Test
    void declineByCustomerChangesStatusToRejected() {
        Quotation quotation = quotation(1, cart(1), List.of(), 0);
        quotation.setStatus(QuotationStatus.PENDING);

        when(quotationRepository.findById(1)).thenReturn(Optional.of(quotation));
        when(quotationRepository.save(any(Quotation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Quotation result = service.declineByCustomer(1);

        assertThat(result.getStatus()).isEqualTo(QuotationStatus.REJECTED);
        assertThat(result.getResponseAt()).isNotNull();
    }

    @Test
    void declineByCustomerThrowsWhenNotPending() {
        Quotation quotation = quotation(1, cart(1), List.of(), 0);
        quotation.setStatus(QuotationStatus.REJECTED);

        when(quotationRepository.findById(1)).thenReturn(Optional.of(quotation));

        assertThatThrownBy(() -> service.declineByCustomer(1))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("pending quotations");
    }

// =========================================================================
// findByCustomerAndStore / findByCustomerInStore
// =========================================================================

    @Test
    void findByCustomerAndStoreDelegatesToCustomerStoreQuery() {
        Quotation quotation = quotation(1, cart(1), List.of(), 0);
        quotation.setStatus(QuotationStatus.PENDING);

        when(quotationRepository.findByCustomerIdAndStoreId(1, 10))
                .thenReturn(List.of(quotation));

        List<Quotation> result = service.findByCustomerAndStore(1, 10);

        assertThat(result).containsExactly(quotation);
    }

    @Test
    void findByCustomerAndStoreListsQuotationEvenWhenCartItemsAreEmptyOrInconsistent() {
        ShoppingCart emptyCart = cart(1);
        emptyCart.setItems(new ArrayList<>());
        Quotation quotation = quotation(2, emptyCart, List.of(), 0);
        quotation.setStatus(QuotationStatus.APPROVED);

        when(quotationRepository.findByCustomerIdAndStoreId(1, 10))
                .thenReturn(List.of(quotation));

        assertThat(service.findByCustomerAndStore(1, 10)).containsExactly(quotation);
    }

    @Test
    void findByCustomerInStoreThrowsWhenNotFound() {
        when(quotationRepository.findByCustomerIdAndStoreId(1, 10)).thenReturn(List.of());

        assertThatThrownBy(() -> service.findByCustomerInStore(99, 1, 10))
                .isInstanceOf(ResourceNotFoundException.class);
    }


}
