package pe.edu.pucp.kingstore.service.product;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.pucp.kingstore.domain.dto.product.ProductRequestDTO;
import pe.edu.pucp.kingstore.domain.model.cart.ShoppingCart;
import pe.edu.pucp.kingstore.domain.model.product.Discount;
import pe.edu.pucp.kingstore.domain.model.product.Product;
import pe.edu.pucp.kingstore.domain.model.product.ProductVariant;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.domain.model.user.UserAccount;
import pe.edu.pucp.kingstore.repository.cart.ShoppingCartRepository;
import pe.edu.pucp.kingstore.repository.product.DiscountRepository;
import pe.edu.pucp.kingstore.repository.product.ProductRepository;
import pe.edu.pucp.kingstore.repository.product.ProductTestDataUtil;
import pe.edu.pucp.kingstore.repository.quotation.QuotationRepository;
import pe.edu.pucp.kingstore.repository.quotation.QuotationTestDataUtil;
import pe.edu.pucp.kingstore.repository.store.StoreRepository;
import pe.edu.pucp.kingstore.repository.user.CustomerRepository;
import pe.edu.pucp.kingstore.repository.user.UserAccountRepository;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private DiscountRepository discountRepository;
    @Autowired
    private StoreRepository storeRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ShoppingCartRepository shoppingCartRepository;
    @Autowired
    private QuotationRepository quotationRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void updateReferencedProductWithoutCommercialChangesKeepsProductAndVariantIds() {
        ReferencedProduct data = referencedProduct();
        entityManager.flush();
        Integer productId = data.product().getId();
        var variantIds = data.product().getVariants().stream()
                .map(ProductVariant::getId)
                .toList();

        ProductRequestDTO request = requestFrom(data.product());
        assertThat((Boolean) ReflectionTestUtils.invokeMethod(
                productService, "isInventoryOnlyChange", data.product(), request)).isTrue();

        Product updated = productService.updateForStore(data.product(), request);
        entityManager.flush();

        assertThat(updated.getId()).isEqualTo(productId);
        assertThat(updated.getDeleted()).isFalse();
        assertThat(updated.getVariants())
                .extracting(ProductVariant::getId)
                .containsExactlyElementsOf(variantIds);
        assertThat(quotationRepository.findById(data.quotation().getId())).isPresent();
    }

    @Test
    void updateReferencedProductWithCommercialChangeCopiesProductAndDiscountWithoutHardDelete() {
        ReferencedProduct data = referencedProduct();
        Discount discount = ProductTestDataUtil.createTestDiscountA(data.product());
        discount.setStore(data.store());
        discount.setActive(true);
        discount = discountRepository.save(discount);
        entityManager.flush();
        Integer oldProductId = data.product().getId();
        Integer oldDiscountId = discount.getId();
        double discountPercentage = discount.getDiscountPercentage();

        ProductRequestDTO request = requestFrom(data.product());
        request.setName(data.product().getName() + " actualizado");

        Product replacement = productService.updateForStore(data.product(), request);
        entityManager.flush();

        assertThat(replacement.getId()).isNotEqualTo(oldProductId);
        assertThat(replacement.getName()).isEqualTo(request.getName());
        assertThat(productRepository.findById(oldProductId)).isPresent();
        assertThat(data.product().getDeleted()).isTrue();
        assertThat(data.product().getReplacedByProduct().getId()).isEqualTo(replacement.getId());
        assertThat(discountRepository.findById(oldDiscountId)).isPresent();
        assertThat(discount.getDeleted()).isTrue();
        assertThat(discount.getActive()).isFalse();
        assertThat(discountRepository.findByProductId(replacement.getId()))
                .singleElement()
                .satisfies(copy -> {
                    assertThat(copy.getId()).isNotEqualTo(oldDiscountId);
                    assertThat(copy.getActive()).isTrue();
                    assertThat(copy.getDeleted()).isFalse();
                    assertThat(copy.getDiscountPercentage()).isEqualTo(discountPercentage);
                });
        assertThat(quotationRepository.findById(data.quotation().getId())).isPresent();
    }

    private ReferencedProduct referencedProduct() {
        Store store = storeRepository.save(ProductTestDataUtil.createTestStoreA());
        Product product = productRepository.save(ProductTestDataUtil.createTestProductA(store));
        UserAccount userAccount = userAccountRepository.save(QuotationTestDataUtil.createUserAccountA());
        Customer customer = QuotationTestDataUtil.createCustomerA(userAccount);
        customer.setStore(store);
        customer = customerRepository.save(customer);
        ShoppingCart shoppingCart = shoppingCartRepository.save(
                QuotationTestDataUtil.createShoppingCartA(customer, product));
        Quotation quotation = quotationRepository.save(
                QuotationTestDataUtil.createQuotationA(shoppingCart, product));
        return new ReferencedProduct(store, product, quotation);
    }

    private ProductRequestDTO requestFrom(Product product) {
        ProductRequestDTO request = new ProductRequestDTO();
        request.setName(product.getName());
        request.setDescription(product.getDescription());
        request.setPrice(product.getBasePrice());
        request.setCostPrice(product.getCostPrice());
        request.setImageUrls(product.getImageUrls());
        request.setCustomizable(product.getCustomizable());
        request.setActive(product.getActive());
        request.setStatus("ACTIVE");
        request.setVariants(product.getVariants().stream().map(variant -> {
            ProductRequestDTO.ProductVariantRequestDTO item =
                    new ProductRequestDTO.ProductVariantRequestDTO();
            item.setSize(variant.getSize());
            item.setColor(variant.getColor() == null ? Color.BLACK : variant.getColor());
            item.setStock(variant.getStock());
            return item;
        }).toList());
        return request;
    }

    private record ReferencedProduct(Store store, Product product, Quotation quotation) {
    }
}
