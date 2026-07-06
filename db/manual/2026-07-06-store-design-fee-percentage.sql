-- KingStore manual migration
-- Purpose: persist the store-level design/customization fee percentage.
-- Target: MySQL 8.x
-- Safe to rerun: adds the column only when missing and backfills nulls to 10.

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

CALL ks_add_column_if_missing('store', 'design_fee_percentage', 'DOUBLE NOT NULL DEFAULT 10');

UPDATE store
SET design_fee_percentage = 10
WHERE design_fee_percentage IS NULL;

DROP PROCEDURE IF EXISTS ks_add_column_if_missing;
