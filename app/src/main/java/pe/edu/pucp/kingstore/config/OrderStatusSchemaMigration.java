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
public class OrderStatusSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public OrderStatusSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            if (!isMySql()) {
                return;
            }

            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                    "SHOW COLUMNS FROM purchase_order LIKE 'status'");
            if (columns.isEmpty()) {
                return;
            }

            Object typeValue = columns.get(0).get("Type");
            String type = typeValue == null ? "" : String.valueOf(typeValue)
                    .toLowerCase(Locale.ROOT);
            if (type.startsWith("enum(")) {
                jdbcTemplate.execute(
                        "ALTER TABLE purchase_order MODIFY COLUMN status VARCHAR(32) NOT NULL");
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to migrate purchase_order.status from legacy MySQL ENUM to VARCHAR(32)",
                    e);
        }
    }

    private boolean isMySql() throws Exception {
        var dataSource = jdbcTemplate.getDataSource();
        if (dataSource == null) {
            return false;
        }
        try (Connection connection = dataSource.getConnection()) {
            String productName = connection.getMetaData().getDatabaseProductName();
            return productName != null
                    && productName.toLowerCase(Locale.ROOT).contains("mysql");
        }
    }
}
