-- Las categorías ahora son globales para todas las tiendas.
-- En algunas BD quedó una columna heredada store_category.store_id.
-- El modelo actual ya no la usa, porque la relación correcta es:
-- store.store_category_id -> store_category.id.

DROP PROCEDURE IF EXISTS ks_drop_store_category_store_id;

DELIMITER //
CREATE PROCEDURE ks_drop_store_category_store_id()
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'store_category'
          AND column_name = 'store_id'
    ) THEN
        ALTER TABLE store_category DROP COLUMN store_id;
    END IF;
END //
DELIMITER ;

CALL ks_drop_store_category_store_id();
DROP PROCEDURE IF EXISTS ks_drop_store_category_store_id;
