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
public class ShippingDetailSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public ShippingDetailSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            if (!isMySql()) {
                return;
            }
            addColumnIfMissing("shipping_detail", "recipient_name", "VARCHAR(150) NULL");
            addColumnIfMissing("shipping_detail", "phone", "VARCHAR(20) NULL");
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to ensure shipping_detail recipient/phone columns",
                    e);
        }
    }

    private void addColumnIfMissing(String tableName, String columnName, String definition) {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SHOW COLUMNS FROM " + tableName + " LIKE ?",
                columnName);
        if (columns.isEmpty()) {
            jdbcTemplate.execute("ALTER TABLE " + tableName
                    + " ADD COLUMN " + columnName + " " + definition);
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
