package pe.edu.pucp.kingstore.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ShippingDetailSchemaMigration implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ShippingDetailSchemaMigration.class);

    private final JdbcTemplate jdbcTemplate;

    public ShippingDetailSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!isMySql()) {
            return;
        }
        try {
            ensureShippingDetailTable();
            ensureShippingDetailColumns();
            ensureOrderFkColumn();
            ensureOrderShippingDetailForeignKey();
        } catch (Exception e) {
            log.error("ShippingDetailSchemaMigration failed: {}", e.getMessage(), e);
            throw new IllegalStateException("Failed to ensure shipping_detail schema", e);
        }
    }

    private void ensureShippingDetailTable() {
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME FROM information_schema.TABLES "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shipping_detail'");
        if (tables.isEmpty()) {
            log.info("Creating shipping_detail table");
            jdbcTemplate.execute(
                    "CREATE TABLE shipping_detail ("
                            + "id INT AUTO_INCREMENT PRIMARY KEY, "
                            + "active BIT(1) NOT NULL DEFAULT 1, "
                            + "address VARCHAR(255) NOT NULL, "
                            + "district VARCHAR(255) NOT NULL, "
                            + "description VARCHAR(500) NULL, "
                            + "estimated_delivery_date DATE NULL, "
                            + "actual_delivery_date DATE NULL, "
                            + "recipient_name VARCHAR(150) NULL, "
                            + "phone VARCHAR(20) NULL"
                            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        }
    }

    private void ensureShippingDetailColumns() {
        ensureShippingDetailIdColumn();
        addColumnIfMissing("shipping_detail", "active", "BIT(1) NOT NULL DEFAULT 1");
        addColumnIfMissing("shipping_detail", "address", "VARCHAR(255) NOT NULL DEFAULT ''");
        addColumnIfMissing("shipping_detail", "district", "VARCHAR(255) NOT NULL DEFAULT 'OTRO'");
        addColumnIfMissing("shipping_detail", "description", "VARCHAR(500) NULL");
        addColumnIfMissing("shipping_detail", "estimated_delivery_date", "DATE NULL");
        addColumnIfMissing("shipping_detail", "actual_delivery_date", "DATE NULL");
        addColumnIfMissing("shipping_detail", "recipient_name", "VARCHAR(150) NULL");
        addColumnIfMissing("shipping_detail", "phone", "VARCHAR(20) NULL");
    }

    private void ensureShippingDetailIdColumn() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SHOW COLUMNS FROM shipping_detail LIKE 'id'");
        if (!columns.isEmpty()) {
            return;
        }

        List<Map<String, Object>> primaryKeys = jdbcTemplate.queryForList(
                "SHOW INDEX FROM shipping_detail WHERE Key_name = 'PRIMARY'");
        if (!primaryKeys.isEmpty()) {
            log.warn("shipping_detail.id is missing but another primary key exists; skipping unsafe id migration");
            return;
        }

        log.info("Adding shipping_detail.id primary key column");
        jdbcTemplate.execute(
                "ALTER TABLE shipping_detail ADD COLUMN id INT AUTO_INCREMENT PRIMARY KEY FIRST");
    }

    private void ensureOrderFkColumn() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SHOW COLUMNS FROM purchase_order LIKE 'shipping_detail_id'");
        if (columns.isEmpty()) {
            log.info("Adding shipping_detail_id column to purchase_order");
            jdbcTemplate.execute(
                    "ALTER TABLE purchase_order ADD COLUMN shipping_detail_id INT NULL");
        }
    }

    private void ensureOrderShippingDetailForeignKey() {
        Integer existingFk = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'purchase_order'
                  AND COLUMN_NAME = 'shipping_detail_id'
                  AND REFERENCED_TABLE_NAME = 'shipping_detail'
                  AND REFERENCED_COLUMN_NAME = 'id'
                """, Integer.class);
        if (existingFk != null && existingFk > 0) {
            return;
        }

        Integer orphanCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM purchase_order po
                LEFT JOIN shipping_detail sd ON sd.id = po.shipping_detail_id
                WHERE po.shipping_detail_id IS NOT NULL
                  AND sd.id IS NULL
                """, Integer.class);
        if (orphanCount != null && orphanCount > 0) {
            log.warn("Skipping purchase_order.shipping_detail_id FK because {} orphan references exist", orphanCount);
            return;
        }

        addIndexIfMissing("purchase_order", "idx_purchase_order_shipping_detail_id", "shipping_detail_id");
        log.info("Adding FK purchase_order.shipping_detail_id -> shipping_detail.id");
        jdbcTemplate.execute(
                "ALTER TABLE purchase_order ADD CONSTRAINT fk_purchase_order_shipping_detail "
                        + "FOREIGN KEY (shipping_detail_id) REFERENCES shipping_detail (id)");
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SHOW COLUMNS FROM " + tableName + " LIKE ?",
                columnName);
        if (columns.isEmpty()) {
            log.info("Adding column {}.{}", tableName, columnName);
            jdbcTemplate.execute("ALTER TABLE " + tableName
                    + " ADD COLUMN " + columnName + " " + definition);
        }
    }

    private void addIndexIfMissing(String tableName, String indexName, String columnName) {
        List<Map<String, Object>> indexes = jdbcTemplate.queryForList(
                "SELECT INDEX_NAME FROM information_schema.STATISTICS "
                        + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?",
                tableName,
                indexName);
        if (indexes.isEmpty()) {
            log.info("Adding index {}.{}", tableName, indexName);
            jdbcTemplate.execute("ALTER TABLE " + tableName
                    + " ADD INDEX " + indexName + " (" + columnName + ")");
        }
    }

    private boolean isMySql() throws Exception {
        var dataSource = jdbcTemplate.getDataSource();
        if (dataSource == null) {
            return false;
        }
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            return isMySqlCompatible(productName);
        }
    }

    static boolean isMySqlCompatible(String productName) {
        if (productName == null) {
            return false;
        }
        String normalized = productName.toLowerCase(Locale.ROOT);
        return normalized.contains("mysql") || normalized.contains("mariadb");
    }
}
