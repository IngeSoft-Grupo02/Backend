package pe.edu.pucp.kingstore.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class ProductSoftDeleteSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public ProductSoftDeleteSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            if (!isMySql()) {
                return;
            }

            addColumnIfMissing("deleted", "ALTER TABLE product ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0");
            addColumnIfMissing("deleted_at", "ALTER TABLE product ADD COLUMN deleted_at DATETIME NULL");
            addColumnIfMissing("replaced_by_product_id", "ALTER TABLE product ADD COLUMN replaced_by_product_id INT NULL");
            jdbcTemplate.execute("UPDATE product SET deleted = 0 WHERE deleted IS NULL");
            addIndexIfMissing("idx_product_deleted",
                    "ALTER TABLE product ADD INDEX idx_product_deleted (deleted)");
            addIndexIfMissing("idx_product_replaced_by_product_id",
                    "ALTER TABLE product ADD INDEX idx_product_replaced_by_product_id (replaced_by_product_id)");
            addForeignKeyIfMissing("fk_product_replaced_by_product",
                    "ALTER TABLE product ADD CONSTRAINT fk_product_replaced_by_product "
                            + "FOREIGN KEY (replaced_by_product_id) REFERENCES product (id)");
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to ensure product soft-delete and replacement columns",
                    e);
        }
    }

    private void addColumnIfMissing(String columnName, String ddl) {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SHOW COLUMNS FROM product LIKE '" + columnName + "'");
        if (columns.isEmpty()) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void addIndexIfMissing(String indexName, String ddl) {
        List<Map<String, Object>> indexes = jdbcTemplate.queryForList(
                "SHOW INDEX FROM product WHERE Key_name = '" + indexName + "'");
        if (indexes.isEmpty()) {
            jdbcTemplate.execute(ddl);
        }
    }

    private void addForeignKeyIfMissing(String constraintName, String ddl) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.KEY_COLUMN_USAGE
                WHERE TABLE_SCHEMA = DATABASE()
                  AND TABLE_NAME = 'product'
                  AND CONSTRAINT_NAME = ?
                """, Integer.class, constraintName);
        if (count == null || count == 0) {
            jdbcTemplate.execute(ddl);
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
