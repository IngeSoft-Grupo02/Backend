-- Full shipping_detail schema: table, columns, and FK on purchase_order.
-- Idempotent: checks existence before each operation.
-- No data deletion, no hard delete.

-- 1. Create shipping_detail table if it does not exist
CREATE TABLE IF NOT EXISTS shipping_detail (
    id INT AUTO_INCREMENT PRIMARY KEY,
    active BIT(1) NOT NULL DEFAULT 1,
    address VARCHAR(255) NOT NULL,
    district VARCHAR(255) NOT NULL,
    description VARCHAR(500) NULL,
    estimated_delivery_date DATE NULL,
    actual_delivery_date DATE NULL,
    recipient_name VARCHAR(150) NULL,
    phone VARCHAR(20) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Add columns to shipping_detail if missing

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shipping_detail' AND COLUMN_NAME = 'id');
SET @pk = (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shipping_detail' AND INDEX_NAME = 'PRIMARY');
SET @sql = IF(@col = 0 AND @pk = 0, 'ALTER TABLE shipping_detail ADD COLUMN id INT AUTO_INCREMENT PRIMARY KEY FIRST', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shipping_detail' AND COLUMN_NAME = 'active');
SET @sql = IF(@col = 0, 'ALTER TABLE shipping_detail ADD COLUMN active BIT(1) NOT NULL DEFAULT 1', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shipping_detail' AND COLUMN_NAME = 'address');
SET @sql = IF(@col = 0, 'ALTER TABLE shipping_detail ADD COLUMN address VARCHAR(255) NOT NULL DEFAULT ''''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shipping_detail' AND COLUMN_NAME = 'district');
SET @sql = IF(@col = 0, 'ALTER TABLE shipping_detail ADD COLUMN district VARCHAR(255) NOT NULL DEFAULT ''OTRO''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @district_fix = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'shipping_detail'
    AND COLUMN_NAME = 'district'
    AND (DATA_TYPE <> 'varchar'
      OR CHARACTER_MAXIMUM_LENGTH < 255
      OR IS_NULLABLE <> 'NO'));
SET @sql = IF(@district_fix > 0, 'UPDATE shipping_detail SET district = ''OTRO'' WHERE district IS NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @sql = IF(@district_fix > 0, 'ALTER TABLE shipping_detail MODIFY COLUMN district VARCHAR(255) NOT NULL DEFAULT ''OTRO''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shipping_detail' AND COLUMN_NAME = 'description');
SET @sql = IF(@col = 0, 'ALTER TABLE shipping_detail ADD COLUMN description VARCHAR(500) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shipping_detail' AND COLUMN_NAME = 'estimated_delivery_date');
SET @sql = IF(@col = 0, 'ALTER TABLE shipping_detail ADD COLUMN estimated_delivery_date DATE NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shipping_detail' AND COLUMN_NAME = 'actual_delivery_date');
SET @sql = IF(@col = 0, 'ALTER TABLE shipping_detail ADD COLUMN actual_delivery_date DATE NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shipping_detail' AND COLUMN_NAME = 'recipient_name');
SET @sql = IF(@col = 0, 'ALTER TABLE shipping_detail ADD COLUMN recipient_name VARCHAR(150) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'shipping_detail' AND COLUMN_NAME = 'phone');
SET @sql = IF(@col = 0, 'ALTER TABLE shipping_detail ADD COLUMN phone VARCHAR(20) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. Add shipping_detail_id FK column to purchase_order if missing

SET @col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'purchase_order' AND COLUMN_NAME = 'shipping_detail_id');
SET @sql = IF(@col = 0, 'ALTER TABLE purchase_order ADD COLUMN shipping_detail_id INT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. Add index/FK only when it is safe

SET @idx = (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'purchase_order' AND INDEX_NAME = 'idx_purchase_order_shipping_detail_id');
SET @sql = IF(@idx = 0, 'ALTER TABLE purchase_order ADD INDEX idx_purchase_order_shipping_detail_id (shipping_detail_id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @fk = (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'purchase_order'
    AND COLUMN_NAME = 'shipping_detail_id'
    AND REFERENCED_TABLE_NAME = 'shipping_detail'
    AND REFERENCED_COLUMN_NAME = 'id');
SET @orphans = (SELECT COUNT(*)
  FROM purchase_order po
  LEFT JOIN shipping_detail sd ON sd.id = po.shipping_detail_id
  WHERE po.shipping_detail_id IS NOT NULL
    AND sd.id IS NULL);
SET @sql = IF(@fk = 0 AND @orphans = 0,
  'ALTER TABLE purchase_order ADD CONSTRAINT fk_purchase_order_shipping_detail FOREIGN KEY (shipping_detail_id) REFERENCES shipping_detail (id)',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
