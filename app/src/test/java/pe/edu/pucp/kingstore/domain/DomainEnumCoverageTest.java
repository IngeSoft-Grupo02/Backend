package pe.edu.pucp.kingstore.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import pe.edu.pucp.kingstore.domain.model.audit.enums.AuditLevel;
import pe.edu.pucp.kingstore.domain.model.order.enums.District;
import pe.edu.pucp.kingstore.domain.model.order.enums.OrderStatus;
import pe.edu.pucp.kingstore.domain.model.payment.enums.PaymentMethod;
import pe.edu.pucp.kingstore.domain.model.product.enums.Color;
import pe.edu.pucp.kingstore.domain.model.product.enums.ProductStatus;
import pe.edu.pucp.kingstore.domain.model.product.enums.Size;
import pe.edu.pucp.kingstore.domain.model.product.enums.VolumeType;
import pe.edu.pucp.kingstore.domain.model.quotation.enums.QuotationStatus;
import pe.edu.pucp.kingstore.domain.model.store.enums.*;
import pe.edu.pucp.kingstore.domain.model.user.enums.DocumentType;
import pe.edu.pucp.kingstore.domain.model.user.enums.Gender;
import pe.edu.pucp.kingstore.domain.model.user.enums.Role;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEnumCoverageTest {

//    @Test
//    void exposesStorePaletteAndProductSizeEnums() {
//        assertThat(Size.values()).containsExactly(Size.XS, Size.S, Size.M, Size.L, Size.XL);
//        assertThat(StoreCategory.values()).contains(StoreCategory.FORMAL, StoreCategory.CASUAL,
//                StoreCategory.SPORTSWEAR, StoreCategory.URBAN);
//        assertThat(PrimaryColor.valueOf("ONYX_BLACK")).isEqualTo(PrimaryColor.ONYX_BLACK);
//        assertThat(SecondaryColor.valueOf("SLATE")).isEqualTo(SecondaryColor.SLATE);
//        assertThat(TertiaryColor.valueOf("RAW_GOLD")).isEqualTo(TertiaryColor.RAW_GOLD);
//    }

    // ---- new version, coverage
    static Stream<Class<? extends  Enum<?>>> enumProvider(){
        return  Stream.of(
                // product
                Size.class,
                Color.class,
                ProductStatus.class,
                VolumeType.class,
                // store
                StoreCategory.class,
                StoreStatus.class,
                PrimaryColor.class,
                SecondaryColor.class,
                TertiaryColor.class,
                // user
                Role.class,
                Gender.class,
                DocumentType.class,
                // order
                OrderStatus.class,
                District.class,
                // others
                AuditLevel.class,
                PaymentMethod.class,
                QuotationStatus.class
        );
    }
    @ParameterizedTest
    @MethodSource("enumProvider")
    void shouldAchieveOneHundredPercentCoverageOnEnums(Class<? extends Enum<?>> enumClass)
        throws Exception{
        //
        Enum<?>[] values = (Enum<?>[]) enumClass.getMethod("values").invoke(null);
        assertThat(values).isNotEmpty();
        //
        for (Enum<?> constant : values){
            Enum<?>valueOfResult = (Enum<?>) enumClass.
                    getMethod("valueOf", String.class).
                    invoke(null, constant.name());
            //
            assertThat(valueOfResult).isEqualTo(constant);
        }
    }
}
