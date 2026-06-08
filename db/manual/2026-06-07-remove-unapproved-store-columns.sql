-- Limpia solo las columnas agregadas por error al modelo store.
-- Hibernate ddl-auto=update no elimina columnas existentes al quitar campos del entity.

DROP PROCEDURE IF EXISTS ks_drop_column_if_exists;

DELIMITER $$
CREATE PROCEDURE ks_drop_column_if_exists(
    IN table_name_value VARCHAR(64),
    IN column_name_value VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = table_name_value
          AND column_name = column_name_value
    ) THEN
        SET @drop_sql = CONCAT(
            'ALTER TABLE `', table_name_value,
            '` DROP COLUMN `', column_name_value, '`'
        );
        PREPARE stmt FROM @drop_sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL ks_drop_column_if_exists('store', 'address');
CALL ks_drop_column_if_exists('store', 'color_palette');
CALL ks_drop_column_if_exists('store', 'contact_email');
CALL ks_drop_column_if_exists('store', 'contact_phone');
CALL ks_drop_column_if_exists('store', 'customization_increment');
CALL ks_drop_column_if_exists('store', 'palette');
CALL ks_drop_column_if_exists('store', 'store_type');
CALL ks_drop_column_if_exists('store', 'website');

DROP PROCEDURE IF EXISTS ks_drop_column_if_exists;
