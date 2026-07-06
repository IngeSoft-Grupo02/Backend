-- KingStore database cleanup audit
-- Purpose: inspect the live schema/data before running destructive cleanup.
-- Target: MySQL 8.x
-- Safe to run: read-only queries only.

-- 1) Tables that exist in the database but are not mapped by the current JPA model.
WITH expected_tables AS (
    SELECT 'attribute' AS table_name UNION ALL
    SELECT 'audit_log' UNION ALL
    SELECT 'cart_item' UNION ALL
    SELECT 'custom_design' UNION ALL
    SELECT 'customer' UNION ALL
    SELECT 'discount' UNION ALL
    SELECT 'merchant' UNION ALL
    SELECT 'password_reset_token' UNION ALL
    SELECT 'payment_receipt' UNION ALL
    SELECT 'person' UNION ALL
    SELECT 'product' UNION ALL
    SELECT 'product_image' UNION ALL
    SELECT 'product_variant' UNION ALL
    SELECT 'purchase_order' UNION ALL
    SELECT 'quotation' UNION ALL
    SELECT 'quotation_design' UNION ALL
    SELECT 'quotation_item' UNION ALL
    SELECT 'shipping_detail' UNION ALL
    SELECT 'shopping_cart' UNION ALL
    SELECT 'store' UNION ALL
    SELECT 'store_category' UNION ALL
    SELECT 'system_administrator' UNION ALL
    SELECT 'user_account'
)
SELECT t.TABLE_NAME AS unexpected_table
FROM information_schema.TABLES t
LEFT JOIN expected_tables e ON e.table_name = t.TABLE_NAME
WHERE t.TABLE_SCHEMA = DATABASE()
  AND t.TABLE_TYPE = 'BASE TABLE'
  AND e.table_name IS NULL
ORDER BY t.TABLE_NAME;

-- 2) Legacy columns known to be unused by the current model.
SELECT c.TABLE_NAME, c.COLUMN_NAME, c.COLUMN_TYPE
FROM information_schema.COLUMNS c
WHERE c.TABLE_SCHEMA = DATABASE()
  AND (
      (c.TABLE_NAME = 'store' AND c.COLUMN_NAME IN (
          'address',
          'color_palette',
          'contact_email',
          'contact_phone',
          'customization_increment',
          'palette',
          'store_type',
          'website'
      ))
      OR (c.TABLE_NAME = 'store_category' AND c.COLUMN_NAME = 'store_id')
      OR (c.TABLE_NAME = 'product' AND c.COLUMN_NAME IN (
          'color',
          'image_url',
          'material',
          'price',
          'size',
          'sku',
          'stock',
          'tag'
      ))
  )
ORDER BY c.TABLE_NAME, c.COLUMN_NAME;

-- 3) Row counts per core table, to understand what cleanup would affect.
SELECT 'attribute' table_name, COUNT(*) rows_count FROM attribute UNION ALL
SELECT 'audit_log', COUNT(*) FROM audit_log UNION ALL
SELECT 'cart_item', COUNT(*) FROM cart_item UNION ALL
SELECT 'custom_design', COUNT(*) FROM custom_design UNION ALL
SELECT 'customer', COUNT(*) FROM customer UNION ALL
SELECT 'discount', COUNT(*) FROM discount UNION ALL
SELECT 'merchant', COUNT(*) FROM merchant UNION ALL
SELECT 'password_reset_token', COUNT(*) FROM password_reset_token UNION ALL
SELECT 'payment_receipt', COUNT(*) FROM payment_receipt UNION ALL
SELECT 'person', COUNT(*) FROM person UNION ALL
SELECT 'product', COUNT(*) FROM product UNION ALL
SELECT 'product_image', COUNT(*) FROM product_image UNION ALL
SELECT 'product_variant', COUNT(*) FROM product_variant UNION ALL
SELECT 'purchase_order', COUNT(*) FROM purchase_order UNION ALL
SELECT 'quotation', COUNT(*) FROM quotation UNION ALL
SELECT 'quotation_design', COUNT(*) FROM quotation_design UNION ALL
SELECT 'quotation_item', COUNT(*) FROM quotation_item UNION ALL
SELECT 'shipping_detail', COUNT(*) FROM shipping_detail UNION ALL
SELECT 'shopping_cart', COUNT(*) FROM shopping_cart UNION ALL
SELECT 'store', COUNT(*) FROM store UNION ALL
SELECT 'store_category', COUNT(*) FROM store_category UNION ALL
SELECT 'system_administrator', COUNT(*) FROM system_administrator UNION ALL
SELECT 'user_account', COUNT(*) FROM user_account
ORDER BY table_name;

-- 4) Stores that look like test/demo data. Review before deleting.
SELECT s.id,
       s.store_name,
       s.slug,
       s.status,
       s.active,
       s.merchant_id
FROM store s
WHERE LOWER(s.slug) REGEXP '(^test|test-|soft-delete|pricing|bulk|prueba|demo)'
   OR LOWER(s.store_name) REGEXP '(^test|test_|soft delete|pricing|bulk|prueba|demo)'
ORDER BY s.id;

-- 5) Users that look like test/demo data. Review before deleting.
SELECT ua.id,
       ua.email,
       ua.role,
       ua.active
FROM user_account ua
WHERE LOWER(ua.email) REGEXP '(^test|test_|bulk|demo|example\\.com)'
ORDER BY ua.id;

-- 6) Products that look like test/demo data. Review before deleting.
SELECT p.id,
       p.store_id,
       p.name,
       p.status,
       p.active
FROM product p
WHERE LOWER(p.name) REGEXP '(^test|test_|prueba|demo|soft delete|pricing|bulk)'
ORDER BY p.id;
