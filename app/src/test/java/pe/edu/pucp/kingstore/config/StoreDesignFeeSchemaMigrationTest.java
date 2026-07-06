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
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StoreDesignFeeSchemaMigrationTest {

    @Test
    void recognizesMysqlAndMariaDbAsCompatible() {
        assertThat(StoreDesignFeeSchemaMigration.isMySqlCompatible("MySQL")).isTrue();
        assertThat(StoreDesignFeeSchemaMigration.isMySqlCompatible("MariaDB")).isTrue();
        assertThat(StoreDesignFeeSchemaMigration.isMySqlCompatible("PostgreSQL")).isFalse();
        assertThat(StoreDesignFeeSchemaMigration.isMySqlCompatible(null)).isFalse();
    }

    @Test
    void existingColumnBackfillsNullAndInvalidLegacyValues() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);

        when(jdbcTemplate.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MariaDB");
        when(jdbcTemplate.queryForList("SHOW COLUMNS FROM store LIKE 'design_fee_percentage'"))
                .thenReturn(List.of(Map.of("Field", "design_fee_percentage")));

        new StoreDesignFeeSchemaMigration(jdbcTemplate).run(mock(ApplicationArguments.class));

        verify(jdbcTemplate).execute(contains("ROUND(design_fee_percentage, 2) NOT IN"));
        verify(jdbcTemplate).execute("ALTER TABLE store MODIFY COLUMN design_fee_percentage DOUBLE NOT NULL DEFAULT 10");
    }
}
