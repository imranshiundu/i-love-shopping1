ALTER TABLE products ADD COLUMN weight_unit VARCHAR(10);
UPDATE products SET weight_unit = 'kg' WHERE weight IS NOT NULL;
