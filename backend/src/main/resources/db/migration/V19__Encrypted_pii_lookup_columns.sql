-- PII at rest: emails and names move to AES-256-GCM ciphertext, with
-- deterministic HMAC lookup columns for equality searches (login, uniqueness,
-- guest-order claiming). Backfill happens in the V20 Java migration, which
-- has access to the configured encryption key.

ALTER TABLE users ALTER COLUMN email TYPE VARCHAR(512);
ALTER TABLE users ALTER COLUMN name TYPE VARCHAR(512);
ALTER TABLE users ADD COLUMN email_lookup VARCHAR(64);
CREATE UNIQUE INDEX idx_users_email_lookup ON users(email_lookup);

ALTER TABLE orders ALTER COLUMN guest_email TYPE VARCHAR(512);
ALTER TABLE orders ADD COLUMN guest_email_lookup VARCHAR(64);
CREATE INDEX idx_orders_guest_email_lookup ON orders(guest_email_lookup);
