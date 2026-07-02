-- KingStore manual migration
-- Purpose: store the visual position of per-item design overlays.
-- Target: MySQL 8.x
-- Safe to rerun: adds columns only when missing.

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

CALL ks_add_column_if_missing('custom_design', 'overlay_x', 'DOUBLE NULL');
CALL ks_add_column_if_missing('custom_design', 'overlay_y', 'DOUBLE NULL');
CALL ks_add_column_if_missing('custom_design', 'overlay_width', 'DOUBLE NULL');
CALL ks_add_column_if_missing('custom_design', 'overlay_height', 'DOUBLE NULL');

CALL ks_add_column_if_missing('quotation_design', 'overlay_x', 'DOUBLE NULL');
CALL ks_add_column_if_missing('quotation_design', 'overlay_y', 'DOUBLE NULL');
CALL ks_add_column_if_missing('quotation_design', 'overlay_width', 'DOUBLE NULL');
CALL ks_add_column_if_missing('quotation_design', 'overlay_height', 'DOUBLE NULL');

DROP PROCEDURE IF EXISTS ks_add_column_if_missing;
