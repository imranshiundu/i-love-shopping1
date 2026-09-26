-- Widen encrypted columns: AES-256-GCM ciphertext ("enc:v1:" prefix +
-- base64 of IV + payload) needs ~2-4x the plaintext length.
ALTER TABLE sessions ALTER COLUMN ip TYPE VARCHAR(255);
ALTER TABLE sessions ALTER COLUMN user_agent TYPE VARCHAR(1200);

ALTER TABLE addresses ALTER COLUMN name TYPE VARCHAR(400);
ALTER TABLE addresses ALTER COLUMN line1 TYPE VARCHAR(700);
ALTER TABLE addresses ALTER COLUMN line2 TYPE VARCHAR(700);
ALTER TABLE addresses ALTER COLUMN city TYPE VARCHAR(400);
ALTER TABLE addresses ALTER COLUMN state TYPE VARCHAR(400);
ALTER TABLE addresses ALTER COLUMN postal_code TYPE VARCHAR(300);
ALTER TABLE addresses ALTER COLUMN phone TYPE VARCHAR(300);
