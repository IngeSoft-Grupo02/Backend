package pe.edu.pucp.kingstore.domain;

import org.junit.jupiter.api.Test;
import pe.edu.pucp.kingstore.domain.model.audit.AuditLog;
import pe.edu.pucp.kingstore.domain.model.order.Order;
import pe.edu.pucp.kingstore.domain.model.payment.PaymentReceipt;
import pe.edu.pucp.kingstore.domain.model.product.CustomDesign;
import pe.edu.pucp.kingstore.domain.model.quotation.Quotation;
import pe.edu.pucp.kingstore.domain.model.store.Store;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica el comportamiento de las entidades del dominio.
 * Cubre métodos de ciclo de vida JPA (@PrePersist) que no son
 * invocados por tests normales sino por el contexto de persistencia.
 * Si se agrega un @PrePersist en una nueva entidad, añadir su clase aquí.
 */
class DomainEntityCoverageTest {

    @Test
    void onCreateSetsTimestampOnAllEntitiesWithPrePersist() throws Exception {
        Object[][] table = {
                { AuditLog.class,      "getTimestamp"   },
                { CustomDesign.class,  "getSentAt"      },
                { Order.class,         "getCreatedAt"   },
                { PaymentReceipt.class,"getIssueDate"   },
                { Quotation.class,     "getRequestedAt" },
                { Store.class,         "getCreatedAt"   }
        };

        for (Object[] row : table) {
            Class<?> entityClass  = (Class<?>) row[0];
            String   getterName   = (String)   row[1];

            Object instance = entityClass.getDeclaredConstructor().newInstance();

            Method onCreate = entityClass.getDeclaredMethod("onCreate");
            onCreate.setAccessible(true);
            onCreate.invoke(instance);

            Object timestamp = entityClass.getMethod(getterName).invoke(instance);
            assertThat(timestamp)
                    .as("onCreate() de %s debe setear %s", entityClass.getSimpleName(), getterName)
                    .isNotNull();
        }
    }
}