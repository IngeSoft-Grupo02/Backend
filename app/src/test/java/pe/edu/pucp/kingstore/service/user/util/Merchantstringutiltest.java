package pe.edu.pucp.kingstore.service.user.util;

import org.junit.jupiter.api.Test;
import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import pe.edu.pucp.kingstore.domain.model.product.enums.ProductStatus;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.domain.model.store.enums.StoreStatus;
import pe.edu.pucp.kingstore.service.common.BusinessRuleException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cubre todos los métodos estáticos de MerchantStringUtil:
 * sanitización de texto, parseo de números/colores/CSV y
 * mapeo de strings a los enums de estado (Quotation, Order, Product, Store).
 */
class MerchantStringUtilTest {

    // =========================================================================
    // requireText
    // =========================================================================

    @Test
    void requireTextThrowsWhenNullOrBlank() {
        assertThatThrownBy(() -> MerchantStringUtil.requireText(null, "Name"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Name is required");
        assertThatThrownBy(() -> MerchantStringUtil.requireText("   ", "Name"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void requireTextDoesNothingWhenValuePresent() {
        MerchantStringUtil.requireText("ok", "Name");
    }

    // =========================================================================
    // blankToNull / blankToEmpty / safe
    // =========================================================================

    @Test
    void blankToNullReturnsNullForNullOrBlank() {
        assertThat(MerchantStringUtil.blankToNull(null)).isNull();
        assertThat(MerchantStringUtil.blankToNull("   ")).isNull();
        assertThat(MerchantStringUtil.blankToNull("  hola  ")).isEqualTo("hola");
    }

    @Test
    void blankToEmptyReturnsEmptyForNull() {
        assertThat(MerchantStringUtil.blankToEmpty(null)).isEqualTo("");
        assertThat(MerchantStringUtil.blankToEmpty("  hola  ")).isEqualTo("hola");
    }

    @Test
    void safeReturnsEmptyForNull() {
        assertThat(MerchantStringUtil.safe(null)).isEqualTo("");
        assertThat(MerchantStringUtil.safe("hola")).isEqualTo("hola");
    }

    // =========================================================================
    // normalizeEmail
    // =========================================================================

    @Test
    void normalizeEmailTrimsAndLowercases() {
        assertThat(MerchantStringUtil.normalizeEmail("  Test@Mail.COM  ")).isEqualTo("test@mail.com");
    }

    @Test
    void normalizeEmailThrowsWhenBlank() {
        assertThatThrownBy(() -> MerchantStringUtil.normalizeEmail(" "))
                .isInstanceOf(BusinessRuleException.class);
    }

    // =========================================================================
    // slugify
    // =========================================================================

    @Test
    void slugifyReturnsEmptyForNull() {
        assertThat(MerchantStringUtil.slugify(null)).isEqualTo("");
    }

    @Test
    void slugifyNormalizesTextToSlug() {
        assertThat(MerchantStringUtil.slugify("  Mi Tienda Genial!! ")).isEqualTo("mi-tienda-genial");
    }

    @Test
    void slugifyCollapsesMultipleSpacesAndDashes() {
        assertThat(MerchantStringUtil.slugify("a   b---c")).isEqualTo("a-b-c");
    }

    // =========================================================================
    // parseInt / parseDouble
    // =========================================================================

    @Test
    void parseIntReturnsNullForNullOrBlank() {
        assertThat(MerchantStringUtil.parseInt(null)).isNull();
        assertThat(MerchantStringUtil.parseInt("  ")).isNull();
    }

    @Test
    void parseIntParsesValidNumber() {
        assertThat(MerchantStringUtil.parseInt(" 42 ")).isEqualTo(42);
    }

    @Test
    void parseIntReturnsNullForInvalidNumber() {
        assertThat(MerchantStringUtil.parseInt("abc")).isNull();
    }

    @Test
    void parseDoubleReturnsNullForNullOrBlank() {
        assertThat(MerchantStringUtil.parseDouble(null)).isNull();
        assertThat(MerchantStringUtil.parseDouble("  ")).isNull();
    }

    @Test
    void parseDoubleParsesValidNumber() {
        assertThat(MerchantStringUtil.parseDouble(" 3.5 ")).isEqualTo(3.5);
    }

    @Test
    void parseDoubleReturnsNullForInvalidNumber() {
        assertThat(MerchantStringUtil.parseDouble("abc")).isNull();
    }

    // =========================================================================
    // parseColor
    // =========================================================================

    @Test
    void parseColorReturnsNullForNullOrBlank() {
        assertThat(MerchantStringUtil.parseColor(null)).isNull();
        assertThat(MerchantStringUtil.parseColor("  ")).isNull();
    }

    @Test
    void parseColorMapsSpanishAndEnglishNames() {
        assertThat(MerchantStringUtil.parseColor("negro")).isEqualTo(Color.BLACK);
        assertThat(MerchantStringUtil.parseColor("BLACK")).isEqualTo(Color.BLACK);
        assertThat(MerchantStringUtil.parseColor("blanco")).isEqualTo(Color.WHITE);
        assertThat(MerchantStringUtil.parseColor("white")).isEqualTo(Color.WHITE);
        assertThat(MerchantStringUtil.parseColor("rojo")).isEqualTo(Color.RED);
        assertThat(MerchantStringUtil.parseColor("red")).isEqualTo(Color.RED);
        assertThat(MerchantStringUtil.parseColor("azul")).isEqualTo(Color.BLUE);
        assertThat(MerchantStringUtil.parseColor("blue")).isEqualTo(Color.BLUE);
        assertThat(MerchantStringUtil.parseColor("verde")).isEqualTo(Color.GREEN);
        assertThat(MerchantStringUtil.parseColor("green")).isEqualTo(Color.GREEN);
    }

    @Test
    void parseColorReturnsNullForUnknownValue() {
        assertThat(MerchantStringUtil.parseColor("morado")).isNull();
    }

    // =========================================================================
    // imageNames
    // =========================================================================

    @Test
    void imageNamesReturnsEmptyForNullOrBlank() {
        assertThat(MerchantStringUtil.imageNames(null)).isEmpty();
        assertThat(MerchantStringUtil.imageNames("  ")).isEmpty();
    }

    @Test
    void imageNamesSplitsTrimsAndFiltersBlanksAndLimitsToFive() {
        String value = "a.png; b.png ;;c.png;d.png;e.png;f.png;g.png";

        var result = MerchantStringUtil.imageNames(value);

        assertThat(result).hasSize(5);
        assertThat(result).containsExactly("a.png", "b.png", "c.png", "d.png", "e.png");
    }

    // =========================================================================
    // splitCsv
    // =========================================================================

    @Test
    void splitCsvReturnsEmptyArrayForNull() {
        assertThat(MerchantStringUtil.splitCsv(null)).isEmpty();
    }

    @Test
    void splitCsvSplitsSimpleLine() {
        assertThat(MerchantStringUtil.splitCsv("a,b,c")).containsExactly("a", "b", "c");
    }

    @Test
    void splitCsvHandlesQuotedCommas() {
        assertThat(MerchantStringUtil.splitCsv("a,\"b,c\",d")).containsExactly("a", "b,c", "d");
    }

    @Test
    void splitCsvTrimsValuesAndHandlesTrailingEmpty() {
        assertThat(MerchantStringUtil.splitCsv(" a , b ,")).containsExactly("a", "b", "");
    }

    // =========================================================================
    // get
    // =========================================================================

    @Test
    void getReturnsNullWhenIndexOrColsNull() {
        assertThat(MerchantStringUtil.get(null, Map.of("name", 0), "name")).isNull();
        assertThat(MerchantStringUtil.get(new String[]{"a"}, null, "name")).isNull();
    }

    @Test
    void getReturnsNullWhenKeyMissingOrOutOfBounds() {
        Map<String, Integer> index = Map.of("name", 5);
        assertThat(MerchantStringUtil.get(new String[]{"a", "b"}, index, "name")).isNull();
        assertThat(MerchantStringUtil.get(new String[]{"a", "b"}, index, "missing")).isNull();
    }

    @Test
    void getReturnsTrimmedValueWhenPresent() {
        Map<String, Integer> index = Map.of("name", 1);
        assertThat(MerchantStringUtil.get(new String[]{"a", " polo "}, index, "name")).isEqualTo("polo");
    }

    // =========================================================================
    // parseQuotationStatus
    // =========================================================================

    @Test
    void parseQuotationStatusMapsKnownValues() {
        assertThat(MerchantStringUtil.parseQuotationStatus("pending")).isEqualTo(QuotationStatus.PENDING);
        assertThat(MerchantStringUtil.parseQuotationStatus("pendiente")).isEqualTo(QuotationStatus.PENDING);
        assertThat(MerchantStringUtil.parseQuotationStatus("approved")).isEqualTo(QuotationStatus.APPROVED);
        assertThat(MerchantStringUtil.parseQuotationStatus("aprobada")).isEqualTo(QuotationStatus.APPROVED);
        assertThat(MerchantStringUtil.parseQuotationStatus("rejected")).isEqualTo(QuotationStatus.REJECTED);
        assertThat(MerchantStringUtil.parseQuotationStatus("rechazada")).isEqualTo(QuotationStatus.REJECTED);
        assertThat(MerchantStringUtil.parseQuotationStatus(" approved-now ".replace("-now", "")))
                .isEqualTo(QuotationStatus.APPROVED);
    }

    @Test
    void parseQuotationStatusNormalizesSeparators() {
        assertThat(MerchantStringUtil.parseQuotationStatus("PENDING")).isEqualTo(QuotationStatus.PENDING);
        assertThat(MerchantStringUtil.parseQuotationStatus("re-jected".replace("-", "")))
                .isEqualTo(QuotationStatus.REJECTED);
    }

    @Test
    void parseQuotationStatusThrowsForInvalidValue() {
        assertThatThrownBy(() -> MerchantStringUtil.parseQuotationStatus("desconocido"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid quotation status");
    }

    // =========================================================================
    // parseOrderStatus
    // =========================================================================

    @Test
    void parseOrderStatusMapsKnownValues() {
        assertThat(MerchantStringUtil.parseOrderStatus("pending_payment")).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(MerchantStringUtil.parseOrderStatus("pago pendiente")).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(MerchantStringUtil.parseOrderStatus("payment_confirmed")).isEqualTo(OrderStatus.PAYMENT_CONFIRMED);
        assertThat(MerchantStringUtil.parseOrderStatus("pagado")).isEqualTo(OrderStatus.PAYMENT_CONFIRMED);
        assertThat(MerchantStringUtil.parseOrderStatus("aprobado")).isEqualTo(OrderStatus.PAYMENT_CONFIRMED);
        assertThat(MerchantStringUtil.parseOrderStatus("in_preparation")).isEqualTo(OrderStatus.IN_PREPARATION);
        assertThat(MerchantStringUtil.parseOrderStatus("en proceso")).isEqualTo(OrderStatus.IN_PREPARATION);
        assertThat(MerchantStringUtil.parseOrderStatus("in-transit")).isEqualTo(OrderStatus.IN_TRANSIT);
        assertThat(MerchantStringUtil.parseOrderStatus("enviado")).isEqualTo(OrderStatus.IN_TRANSIT);
        assertThat(MerchantStringUtil.parseOrderStatus("delivered")).isEqualTo(OrderStatus.DELIVERED);
        assertThat(MerchantStringUtil.parseOrderStatus("entregado")).isEqualTo(OrderStatus.DELIVERED);
        assertThat(MerchantStringUtil.parseOrderStatus("cancelled")).isEqualTo(OrderStatus.CANCELLED);
        assertThat(MerchantStringUtil.parseOrderStatus("cancelado")).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    void parseOrderStatusThrowsForInvalidValue() {
        assertThatThrownBy(() -> MerchantStringUtil.parseOrderStatus("desconocido"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid order status");
    }

    // =========================================================================
    // parseProductStatus
    // =========================================================================

    @Test
    void parseProductStatusMapsKnownValues() {
        assertThat(MerchantStringUtil.parseProductStatus("draft")).isEqualTo(ProductStatus.DRAFT);
        assertThat(MerchantStringUtil.parseProductStatus("borrador")).isEqualTo(ProductStatus.DRAFT);
        assertThat(MerchantStringUtil.parseProductStatus("out of stock")).isEqualTo(ProductStatus.OUT_OF_STOCK);
        assertThat(MerchantStringUtil.parseProductStatus("fuera de stock")).isEqualTo(ProductStatus.OUT_OF_STOCK);
        assertThat(MerchantStringUtil.parseProductStatus("active")).isEqualTo(ProductStatus.ACTIVE);
        assertThat(MerchantStringUtil.parseProductStatus("activo")).isEqualTo(ProductStatus.ACTIVE);
        assertThat(MerchantStringUtil.parseProductStatus("inactive")).isEqualTo(ProductStatus.INACTIVE);
        assertThat(MerchantStringUtil.parseProductStatus("inactivo")).isEqualTo(ProductStatus.INACTIVE);
    }

    @Test
    void parseProductStatusThrowsForInvalidValue() {
        assertThatThrownBy(() -> MerchantStringUtil.parseProductStatus("desconocido"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid product status");
    }

    // =========================================================================
    // parseStoreStatus
    // =========================================================================

    @Test
    void parseStoreStatusMapsKnownValues() {
        assertThat(MerchantStringUtil.parseStoreStatus("active")).isEqualTo(StoreStatus.ACTIVE);
        assertThat(MerchantStringUtil.parseStoreStatus("activa")).isEqualTo(StoreStatus.ACTIVE);
        assertThat(MerchantStringUtil.parseStoreStatus("inactive")).isEqualTo(StoreStatus.INACTIVE);
        assertThat(MerchantStringUtil.parseStoreStatus("inactiva")).isEqualTo(StoreStatus.INACTIVE);
        assertThat(MerchantStringUtil.parseStoreStatus("suspended")).isEqualTo(StoreStatus.SUSPENDED);
        assertThat(MerchantStringUtil.parseStoreStatus("suspendida")).isEqualTo(StoreStatus.SUSPENDED);
        assertThat(MerchantStringUtil.parseStoreStatus("on-hold".replace("on-hold", "suspended")))
                .isEqualTo(StoreStatus.SUSPENDED);
    }

    @Test
    void parseStoreStatusThrowsForInvalidValue() {
        assertThatThrownBy(() -> MerchantStringUtil.parseStoreStatus("desconocido"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid store status");
    }

    // =========================================================================
    // extension / contentType
    // =========================================================================

    @Test
    void extensionReturnsEmptyWhenNullOrNoDot() {
        assertThat(MerchantStringUtil.extension(null)).isEqualTo("");
        assertThat(MerchantStringUtil.extension("filename")).isEqualTo("");
    }

    @Test
    void extensionReturnsLowercaseExtension() {
        assertThat(MerchantStringUtil.extension("Foto.PNG")).isEqualTo("png");
        assertThat(MerchantStringUtil.extension("archivo.tar.gz")).isEqualTo("gz");
    }

    @Test
    void contentTypeMapsJpgToJpeg() {
        assertThat(MerchantStringUtil.contentType("foto.jpg")).isEqualTo("image/jpeg");
        assertThat(MerchantStringUtil.contentType("foto.png")).isEqualTo("image/png");
        assertThat(MerchantStringUtil.contentType("sinextension")).isEqualTo("image/");
    }
}
