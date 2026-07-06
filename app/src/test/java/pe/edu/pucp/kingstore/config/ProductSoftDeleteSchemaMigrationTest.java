package pe.edu.pucp.kingstore.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductSoftDeleteSchemaMigrationTest {

    @Test
    void isMySqlCompatibleAcceptsMysqlAndMariadbOnly() {
        assertThat(ProductSoftDeleteSchemaMigration.isMySqlCompatible("MySQL")).isTrue();
        assertThat(ProductSoftDeleteSchemaMigration.isMySqlCompatible("MariaDB")).isTrue();
        assertThat(ProductSoftDeleteSchemaMigration.isMySqlCompatible("H2")).isFalse();
        assertThat(ProductSoftDeleteSchemaMigration.isMySqlCompatible(null)).isFalse();
    }
}
