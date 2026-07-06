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
public class StoreDesignFeeSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public StoreDesignFeeSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            if (!isMySql()) {
                return;
            }

            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                    "SHOW COLUMNS FROM store LIKE 'design_fee_percentage'");
            if (columns.isEmpty()) {
                jdbcTemplate.execute(
                        "ALTER TABLE store ADD COLUMN design_fee_percentage DOUBLE NOT NULL DEFAULT 10");
            } else {
                jdbcTemplate.execute("UPDATE store SET design_fee_percentage = 10 WHERE design_fee_percentage IS NULL");
                jdbcTemplate.execute(
                        "ALTER TABLE store MODIFY COLUMN design_fee_percentage DOUBLE NOT NULL DEFAULT 10");
            }
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to ensure store.design_fee_percentage customization fee column",
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
