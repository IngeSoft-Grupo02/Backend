-- KingStore manual migration
-- Purpose: support per-item design comments and file associations in quotations.
-- Target: MySQL 8.x
-- Safe to rerun: adds columns, index and FK only when missing.

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

DROP PROCEDURE IF EXISTS ks_add_index_if_missing//
CREATE PROCEDURE ks_add_index_if_missing(
    IN p_index_name VARCHAR(64),
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND INDEX_NAME = p_index_name
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE `', p_table_name, '` ADD INDEX `', p_index_name, '` (`', p_column_name, '`)'
        );
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DROP PROCEDURE IF EXISTS ks_add_fk_if_missing//
CREATE PROCEDURE ks_add_fk_if_missing(
    IN p_constraint_name VARCHAR(64),
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64),
    IN p_referenced_table_name VARCHAR(64),
    IN p_referenced_column_name VARCHAR(64)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
          AND REFERENCED_TABLE_NAME = p_referenced_table_name
          AND REFERENCED_COLUMN_NAME = p_referenced_column_name
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE `', p_table_name, '` ADD CONSTRAINT `', p_constraint_name, '` ',
            'FOREIGN KEY (`', p_column_name, '`) REFERENCES `', p_referenced_table_name, '`(`', p_referenced_column_name, '`)'
        );
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DELIMITER ;

CALL ks_add_column_if_missing('quotation_item', 'customer_description', 'VARCHAR(500) NULL');
CALL ks_add_column_if_missing('quotation_design', 'quotation_item_id', 'INT NULL');
CALL ks_add_index_if_missing('idx_quotation_design_item_id', 'quotation_design', 'quotation_item_id');
CALL ks_add_fk_if_missing('fk_quotation_design_item', 'quotation_design', 'quotation_item_id', 'quotation_item', 'id');

DROP PROCEDURE IF EXISTS ks_add_fk_if_missing;
DROP PROCEDURE IF EXISTS ks_add_index_if_missing;
DROP PROCEDURE IF EXISTS ks_add_column_if_missing;