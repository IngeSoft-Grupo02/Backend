-- KingStore manual migration
-- Purpose: orders created from accepted quotations must wait for customer payment.
-- Target: MySQL 8.x
-- Safe to rerun: only updates orders without a payment receipt.

UPDATE purchase_order po
LEFT JOIN payment_receipt pr ON pr.order_id = po.id
SET po.status = 'PENDING_PAYMENT'
WHERE po.status = 'PAYMENT_CONFIRMED'
  AND pr.id IS NULL;
