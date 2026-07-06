-- KingStore legacy schema cleanup
-- Purpose: remove columns left by earlier iterations that are not used by the current JPA model.
-- Target: MySQL 8.x
-- Run only after backing up the database and reviewing 2026-07-02-db-cleanup-audit.sql.

DROP PROCEDURE IF EXISTS ks_drop_column_if_exists;

DELIMITER //

CREATE PROCEDURE ks_drop_column_if_exists(
    IN p_table_name VARCHAR(64),
    IN p_column_name VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = p_table_name
          AND COLUMN_NAME = p_column_name
    ) THEN
        SET @ddl = CONCAT(
            'ALTER TABLE `', p_table_name, '` DROP COLUMN `', p_column_name, '`'
        );
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DELIMITER ;

-- Store columns from earlier UI/admin experiments. Current model keeps only:
-- store_name, slug, description, logo_url, created_at, category FK,
-- colors, status and merchant FK.
CALL ks_drop_column_if_exists('store', 'address');
CALL ks_drop_column_if_exists('store', 'color_palette');
CALL ks_drop_column_if_exists('store', 'contact_email');
CALL ks_drop_column_if_exists('store', 'contact_phone');
CALL ks_drop_column_if_exists('store', 'customization_increment');
CALL ks_drop_column_if_exists('store', 'palette');
CALL ks_drop_column_if_exists('store', 'store_type');
CALL ks_drop_column_if_exists('store', 'website');

-- Store categories are global now. The relation is store.store_category_id -> store_category.id.
CALL ks_drop_column_if_exists('store_category', 'store_id');

-- Product catalog now stores stock by product_variant and images by product_image.
-- These columns are legacy if they exist directly on product.
CALL ks_drop_column_if_exists('product', 'color');
CALL ks_drop_column_if_exists('product', 'image_url');
CALL ks_drop_column_if_exists('product', 'material');
CALL ks_drop_column_if_exists('product', 'price');
CALL ks_drop_column_if_exists('product', 'size');
CALL ks_drop_column_if_exists('product', 'sku');
CALL ks_drop_column_if_exists('product', 'stock');
CALL ks_drop_column_if_exists('product', 'tag');

DROP PROCEDURE IF EXISTS ks_drop_column_if_exists;
