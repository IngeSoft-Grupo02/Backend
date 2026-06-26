-- KingStore manual migration
-- Purpose: allow response_at to be NULL on the quotation table.
--          A newly created quotation (status=PENDING) has no response date yet.
--          responseAt is only set when the merchant approves or rejects the quotation.
-- Target: MySQL 8.x
-- Apply against the kingstore database.

ALTER TABLE quotation
    MODIFY COLUMN response_at DATETIME NULL;
