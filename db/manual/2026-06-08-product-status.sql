ALTER TABLE product
    ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE';

UPDATE product
SET status = CASE
    WHEN active = 0 THEN 'INACTIVE'
    ELSE 'ACTIVE'
END;

UPDATE product
SET active = CASE
    WHEN status = 'ACTIVE' THEN 1
    ELSE 0
END;
