-- Adds recipientName and phone to shipping_detail for customer shipping address flow.
-- Idempotent: uses IF NOT EXISTS via information_schema check.
-- No data deletion, no hard delete.

-- recipient_name (varchar 150, nullable)
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'shipping_detail'
    AND COLUMN_NAME = 'recipient_name');

SET @sql = IF(@col_exists = 0,
  'ALTER TABLE shipping_detail ADD COLUMN recipient_name VARCHAR(150) NULL',
  'SELECT ''recipient_name already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- phone (varchar 20, nullable)
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'shipping_detail'
    AND COLUMN_NAME = 'phone');

SET @sql = IF(@col_exists = 0,
  'ALTER TABLE shipping_detail ADD COLUMN phone VARCHAR(20) NULL',
  'SELECT ''phone already exists''');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
