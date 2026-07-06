package pe.edu.pucp.kingstore.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.*;

class ShippingDetailSchemaMigrationTest {

    @Test
    void recognizesMysqlAndMariaDbAsCompatible() {
        assertThat(ShippingDetailSchemaMigration.isMySqlCompatible("MySQL")).isTrue();
        assertThat(ShippingDetailSchemaMigration.isMySqlCompatible("MariaDB")).isTrue();
        assertThat(ShippingDetailSchemaMigration.isMySqlCompatible("PostgreSQL")).isFalse();
        assertThat(ShippingDetailSchemaMigration.isMySqlCompatible(null)).isFalse();
    }

    @Test
    void createsTableWhenMissing() throws Exception {
        JdbcTemplate jdbcTemplate = mysqlJdbcTemplate();

        whenShippingDetailTableExists(jdbcTemplate, false);
        whenShippingDetailIdExists(jdbcTemplate);
        whenShippingDetailColumnsExist(jdbcTemplate);
        whenPurchaseOrderShippingDetailColumnExists(jdbcTemplate);
        whenExistingForeignKey(jdbcTemplate);

        new ShippingDetailSchemaMigration(jdbcTemplate).run(mock(ApplicationArguments.class));

        verify(jdbcTemplate).execute(contains("CREATE TABLE shipping_detail"));
    }

    @Test
    void addsIdColumnWhenMissingAndSafe() throws Exception {
        JdbcTemplate jdbcTemplate = mysqlJdbcTemplate();

        whenShippingDetailTableExists(jdbcTemplate, true);
        when(jdbcTemplate.queryForList("SHOW COLUMNS FROM shipping_detail LIKE 'id'"))
                .thenReturn(List.of());
        when(jdbcTemplate.queryForList("SHOW INDEX FROM shipping_detail WHERE Key_name = 'PRIMARY'"))
                .thenReturn(List.of());
        whenShippingDetailColumnsExist(jdbcTemplate);
        whenPurchaseOrderShippingDetailColumnExists(jdbcTemplate);
        whenExistingForeignKey(jdbcTemplate);

        new ShippingDetailSchemaMigration(jdbcTemplate).run(mock(ApplicationArguments.class));

        verify(jdbcTemplate).execute(contains("ALTER TABLE shipping_detail ADD COLUMN id INT AUTO_INCREMENT PRIMARY KEY"));
    }

    @Test
    void addsFkColumnWhenMissing() throws Exception {
        JdbcTemplate jdbcTemplate = mysqlJdbcTemplate();

        whenShippingDetailTableExists(jdbcTemplate, true);
        whenShippingDetailIdExists(jdbcTemplate);
        whenShippingDetailColumnsExist(jdbcTemplate);
        when(jdbcTemplate.queryForList("SHOW COLUMNS FROM purchase_order LIKE 'shipping_detail_id'"))
                .thenReturn(List.of());
        whenExistingForeignKey(jdbcTemplate);

        new ShippingDetailSchemaMigration(jdbcTemplate).run(mock(ApplicationArguments.class));

        verify(jdbcTemplate).execute(contains("ALTER TABLE purchase_order ADD COLUMN shipping_detail_id"));
    }

    @Test
    void addsIndexAndForeignKeyWhenSafeAndMissing() throws Exception {
        JdbcTemplate jdbcTemplate = mysqlJdbcTemplate();

        whenShippingDetailTableExists(jdbcTemplate, true);
        whenShippingDetailIdExists(jdbcTemplate);
        whenShippingDetailColumnsExist(jdbcTemplate);
        whenPurchaseOrderShippingDetailColumnExists(jdbcTemplate);
        whenMissingForeignKeyAndNoOrphans(jdbcTemplate);
        when(jdbcTemplate.queryForList(
                startsWith("SELECT INDEX_NAME FROM information_schema.STATISTICS"),
                eq("purchase_order"),
                eq("idx_purchase_order_shipping_detail_id")))
                .thenReturn(List.of());

        new ShippingDetailSchemaMigration(jdbcTemplate).run(mock(ApplicationArguments.class));

        verify(jdbcTemplate).execute(contains("ADD INDEX idx_purchase_order_shipping_detail_id"));
        verify(jdbcTemplate).execute(contains("ADD CONSTRAINT fk_purchase_order_shipping_detail"));
    }

    @Test
    void skipsForeignKeyWhenOrphanReferencesExist() throws Exception {
        JdbcTemplate jdbcTemplate = mysqlJdbcTemplate();

        whenShippingDetailTableExists(jdbcTemplate, true);
        whenShippingDetailIdExists(jdbcTemplate);
        whenShippingDetailColumnsExist(jdbcTemplate);
        whenPurchaseOrderShippingDetailColumnExists(jdbcTemplate);
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(*)\nFROM information_schema.KEY_COLUMN_USAGE"),
                eq(Integer.class))).thenReturn(0);
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(*)\nFROM purchase_order po"),
                eq(Integer.class))).thenReturn(2);

        new ShippingDetailSchemaMigration(jdbcTemplate).run(mock(ApplicationArguments.class));

        verify(jdbcTemplate, never()).execute(contains("ADD CONSTRAINT fk_purchase_order_shipping_detail"));
    }

    @Test
    void noOpWhenEverythingExists() throws Exception {
        JdbcTemplate jdbcTemplate = mysqlJdbcTemplate();

        whenShippingDetailTableExists(jdbcTemplate, true);
        whenShippingDetailIdExists(jdbcTemplate);
        whenShippingDetailColumnsExist(jdbcTemplate);
        whenPurchaseOrderShippingDetailColumnExists(jdbcTemplate);
        whenExistingForeignKey(jdbcTemplate);

        new ShippingDetailSchemaMigration(jdbcTemplate).run(mock(ApplicationArguments.class));

        verify(jdbcTemplate, never()).execute(contains("CREATE TABLE"));
        verify(jdbcTemplate, never()).execute(contains("ALTER TABLE"));
    }

    private JdbcTemplate mysqlJdbcTemplate() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        when(jdbcTemplate.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySQL");

        return jdbcTemplate;
    }

    private void whenShippingDetailTableExists(JdbcTemplate jdbcTemplate, boolean exists) {
        when(jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM information_schema.TABLES "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shipping_detail'"))
                .thenReturn(exists ? List.of(Map.of("TABLE_NAME", "shipping_detail")) : List.of());
    }

    private void whenShippingDetailIdExists(JdbcTemplate jdbcTemplate) {
        when(jdbcTemplate.queryForList("SHOW COLUMNS FROM shipping_detail LIKE 'id'"))
                .thenReturn(List.of(Map.of("Field", "id")));
    }

    private void whenShippingDetailColumnsExist(JdbcTemplate jdbcTemplate) {
        when(jdbcTemplate.queryForList(eq("SHOW COLUMNS FROM shipping_detail LIKE ?"), anyString()))
                .thenReturn(List.of(Map.of("Field", "exists")));
    }

    private void whenPurchaseOrderShippingDetailColumnExists(JdbcTemplate jdbcTemplate) {
        when(jdbcTemplate.queryForList("SHOW COLUMNS FROM purchase_order LIKE 'shipping_detail_id'"))
                .thenReturn(List.of(Map.of("Field", "shipping_detail_id")));
    }

    private void whenExistingForeignKey(JdbcTemplate jdbcTemplate) {
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(*)\nFROM information_schema.KEY_COLUMN_USAGE"),
                eq(Integer.class))).thenReturn(1);
    }

    private void whenMissingForeignKeyAndNoOrphans(JdbcTemplate jdbcTemplate) {
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(*)\nFROM information_schema.KEY_COLUMN_USAGE"),
                eq(Integer.class))).thenReturn(0);
        when(jdbcTemplate.queryForObject(startsWith("SELECT COUNT(*)\nFROM purchase_order po"),
                eq(Integer.class))).thenReturn(0);
    }
}
