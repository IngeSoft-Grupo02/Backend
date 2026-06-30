-- KingStore manual migration
-- Purpose: support logical deletion for merchant discounts without reusing active.
-- Target: MySQL 8.x
-- Apply against the kingstore database after backing up production data.

DELIMITER //

DROP PROCEDURE IF EXISTS ks_add_column_if_missing//
CREATE PROCEDURE ks_add_column_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_column_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE `', p_table_name, '` ADD COLUMN `', p_column_name, '` ', p_column_definition
        );
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DELIMITER ;

CALL ks_add_column_if_missing('discount', 'deleted', 'TINYINT(1) NOT NULL DEFAULT 0');
CALL ks_add_column_if_missing('discount', 'deleted_at', 'DATETIME NULL');

DROP PROCEDURE IF EXISTS ks_add_column_if_missing;
