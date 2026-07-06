-- KingStore manual migration
-- Purpose: support logical deletion and product replacement tracking.
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

DROP PROCEDURE IF EXISTS ks_add_index_if_missing//
CREATE PROCEDURE ks_add_index_if_missing(
    IN p_table_name VARCHAR(64),
    IN p_index_name VARCHAR(64),
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
    IN p_ref_table_name VARCHAR(64),
    IN p_ref_column_name VARCHAR(64)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND CONSTRAINT_NAME = p_constraint_name
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE `', p_table_name, '` ADD CONSTRAINT `', p_constraint_name, '` ',
            'FOREIGN KEY (`', p_column_name, '`) REFERENCES `', p_ref_table_name, '` (`', p_ref_column_name, '`)'
        );
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DELIMITER ;

CALL ks_add_column_if_missing('product', 'deleted', 'TINYINT(1) NOT NULL DEFAULT 0');
CALL ks_add_column_if_missing('product', 'deleted_at', 'DATETIME NULL');
CALL ks_add_column_if_missing('product', 'replaced_by_product_id', 'INT NULL');
CALL ks_add_index_if_missing('product', 'idx_product_deleted', 'deleted');
CALL ks_add_index_if_missing('product', 'idx_product_replaced_by_product_id', 'replaced_by_product_id');
CALL ks_add_fk_if_missing('fk_product_replaced_by_product', 'product', 'replaced_by_product_id', 'product', 'id');

UPDATE product SET deleted = 0 WHERE deleted IS NULL;

DROP PROCEDURE IF EXISTS ks_add_fk_if_missing;
DROP PROCEDURE IF EXISTS ks_add_index_if_missing;
DROP PROCEDURE IF EXISTS ks_add_column_if_missing;
