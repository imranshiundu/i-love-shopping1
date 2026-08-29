-- Increase order number column length to accommodate 8-char random suffix
ALTER TABLE orders ALTER COLUMN number TYPE VARCHAR(30);
