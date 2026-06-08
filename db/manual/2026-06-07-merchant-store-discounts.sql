-- KingStore manual migration
-- Purpose: support merchant store-level discounts only.
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

DROP PROCEDURE IF EXISTS ks_drop_fks_on_column//
CREATE PROCEDURE ks_drop_fks_on_column(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64)
)
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_constraint_name VARCHAR(64);
    DECLARE fk_cursor CURSOR FOR
        SELECT CONSTRAINT_NAME
        FROM information_schema.KEY_COLUMN_USAGE
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
          AND REFERENCED_TABLE_NAME IS NOT NULL;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN fk_cursor;
    fk_loop: LOOP
        FETCH fk_cursor INTO v_constraint_name;
        IF done = 1 THEN
            LEAVE fk_loop;
        END IF;

        SET @ddl = CONCAT('ALTER TABLE `', p_table_name, '` DROP FOREIGN KEY `', v_constraint_name, '`');
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END LOOP;
    CLOSE fk_cursor;
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
            'FOREIGN KEY (`', p_column_name, '`) REFERENCES `', p_referenced_table_name, '` (`', p_referenced_column_name, '`)'
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
            'CREATE INDEX `', p_index_name, '` ON `', p_table_name, '` (`', p_column_name, '`)'
        );
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DELIMITER ;

-- Store-level discounts. Existing product-level discounts remain supported.
CALL ks_add_column_if_missing('discount', 'store_id', 'INT NULL');
CALL ks_add_column_if_missing('discount', 'name', 'VARCHAR(150) NULL');
CALL ks_add_column_if_missing('discount', 'discount_type', 'VARCHAR(30) NULL');
CALL ks_add_column_if_missing('discount', 'applies_to', 'VARCHAR(100) NULL');
CALL ks_add_column_if_missing('discount', 'usage_count', 'INT NOT NULL DEFAULT 0');

-- Backfill store_id for discounts that were originally tied to a product.
UPDATE discount d
JOIN product p ON p.id = d.product_id
SET d.store_id = p.store_id
WHERE d.store_id IS NULL
  AND d.product_id IS NOT NULL;

-- product_id must be nullable so a discount can apply to the whole store/catalog.
CALL ks_drop_fks_on_column('discount', 'product_id');
ALTER TABLE `discount` MODIFY COLUMN `product_id` INT NULL;
CALL ks_add_fk_if_missing('fk_discount_product', 'discount', 'product_id', 'product', 'id');
CALL ks_add_fk_if_missing('fk_discount_store', 'discount', 'store_id', 'store', 'id');
CALL ks_add_index_if_missing('idx_discount_store_id', 'discount', 'store_id');

DROP PROCEDURE IF EXISTS ks_add_index_if_missing;
DROP PROCEDURE IF EXISTS ks_add_fk_if_missing;
DROP PROCEDURE IF EXISTS ks_drop_fks_on_column;
DROP PROCEDURE IF EXISTS ks_add_column_if_missing;
