package pe.edu.pucp.kingstore.api.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import pe.edu.pucp.kingstore.api.context.MerchantContext;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import pe.edu.pucp.kingstore.domain.model.store.Store;
import pe.edu.pucp.kingstore.domain.model.user.Customer;
import pe.edu.pucp.kingstore.domain.model.user.Merchant;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;
import pe.edu.pucp.kingstore.service.common.ResourceNotFoundException;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaseMerchantControllerCoverageTest {

    private MerchantContext merchantContext;
    private Authentication authentication;
    private TestController controller;

    @BeforeEach
    void setUp() {
        merchantContext = mock(MerchantContext.class);
        authentication = mock(Authentication.class);
        controller = new TestController(merchantContext);
    }

    @Test
    void contextHelpersDelegateToMerchantContext() {
        Merchant merchant = new Merchant();
        Store store = new Store();
        store.setId(10);
        when(merchantContext.merchant(authentication)).thenReturn(merchant);
        when(merchantContext.stores(authentication)).thenReturn(List.of(store));
        when(merchantContext.currentStore(authentication, 10)).thenReturn(store);
        when(merchantContext.userAccountId(authentication)).thenReturn(77);
        when(merchantContext.storeById(authentication, 10)).thenReturn(store);

        assertThat(controller.currentMerchant(authentication)).isSameAs(merchant);
        assertThat(controller.merchantStores(authentication)).containsExactly(store);
        assertThat(controller.currentMerchantStore(authentication, 10)).isSameAs(store);
        assertThat(controller.currentUserAccountId(authentication)).isEqualTo(77);
        assertThat(controller.storeInMerchantScope(authentication, 10)).isSameAs(store);
    }

    @Test
    void handleMapsKnownExceptionsToHttpResponses() {
        assertThat(controller.handle(() -> org.springframework.http.ResponseEntity.ok("ok")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(controller.handle(() -> { throw new ResourceNotFoundException("Store", 1); }).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(controller.handle(() -> { throw new BusinessRuleException("bad"); }).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.handle(() -> { throw new DataIntegrityViolationException("fk"); }).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.handle(() -> { throw new IOException("io"); }).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void textAndParsingHelpersDelegateToUtilities() {
        assertThatThrownBy(() -> controller.requireText(" ", "Nombre"))
                .isInstanceOf(BusinessRuleException.class);
        assertThat(controller.blankToNull("  ")).isNull();
        assertThat(controller.blankToNull("  valor  ")).isEqualTo("valor");
        assertThat(controller.blankToEmpty(null)).isEqualTo("");
        assertThat(controller.safe(null)).isEqualTo("");
        assertThat(controller.normalizeEmail(" USER@MAIL.COM ")).isEqualTo("user@mail.com");
        assertThat(controller.slugify("Mi Tienda Bonita")).isEqualTo("mi-tienda-bonita");
        assertThat(controller.parseInt(" 7 ")).isEqualTo(7);
        assertThat(controller.parseDouble(" 7.5 ")).isEqualTo(7.5);
        assertThat(controller.parseColor("rojo")).isEqualTo(Color.RED);
        assertThat(controller.imageNames("a.png;b.jpg")).contains("a.png", "b.jpg");
        assertThat(controller.splitCsv("a,b")).containsExactly("a", "b");
        assertThat(controller.get(new String[]{"uno"}, Map.of("name", 0), "name")).isEqualTo("uno");
    }

    @Test
    void nameAndFileHelpersReturnExpectedValues() {
        Customer customer = new Customer();
        customer.setFirstName("Ana");
        customer.setPaternalSurname("Perez");
        customer.setMaternalSurname("Rios");

        assertThat(controller.customerName(customer)).contains("Ana");
        assertThat(controller.fullName("Luis", "Lopez", "Mora")).isEqualTo("Luis Lopez Mora");
        assertThat(controller.extension("foto.png")).isEqualTo("png");
        assertThat(controller.extension(null)).isEqualTo("");
        assertThat(controller.contentType("foto.png")).isEqualTo("image/png");
        assertThat(controller.contentType("archivo.bin")).isEqualTo("image/bin");
    }

    private static class TestController extends BaseMerchantController {
        TestController(MerchantContext merchantContext) {
            super(merchantContext);
        }
    }
}
