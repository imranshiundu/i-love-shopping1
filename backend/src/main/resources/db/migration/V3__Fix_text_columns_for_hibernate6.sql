-- V3__Fix_text_columns_for_hibernate6.sql
-- Hibernate 6 maps PostgreSQL TEXT to bytea, breaking LOWER() queries.
-- Convert TEXT columns to VARCHAR which Hibernate handles correctly.

ALTER TABLE products ALTER COLUMN description TYPE VARCHAR(10000);
ALTER TABLE brands ALTER COLUMN description TYPE VARCHAR(10000);
ALTER TABLE categories ALTER COLUMN description TYPE VARCHAR(10000);
ALTER TABLE reviews ALTER COLUMN content TYPE VARCHAR(10000);
ALTER TABLE orders ALTER COLUMN notes TYPE VARCHAR(10000);
