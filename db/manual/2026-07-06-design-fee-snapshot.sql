-- KingStore manual migration
-- Purpose: persist the applied design/customization fee snapshot on quotations and orders.
-- Target: MySQL 8.x
-- Safe to rerun: adds nullable columns only when missing and does not modify historical totals.

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

CALL ks_add_column_if_missing('quotation', 'design_fee_total', 'DOUBLE NULL');
CALL ks_add_column_if_missing('quotation', 'design_fee_percentage_applied', 'DOUBLE NULL');
CALL ks_add_column_if_missing('purchase_order', 'design_fee_total', 'DOUBLE NULL');
CALL ks_add_column_if_missing('purchase_order', 'design_fee_percentage_applied', 'DOUBLE NULL');

DROP PROCEDURE IF EXISTS ks_add_column_if_missing;
