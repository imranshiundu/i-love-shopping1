-- V13: persist email-verification and password-reset tokens on users.
-- Previously AuthService generated UUID tokens but never stored them, so
-- verify-email and reset-password endpoints were no-ops. These columns make
-- tokens single-use (cleared after use) with an expiry timestamp.
ALTER TABLE users ADD COLUMN email_verification_token VARCHAR(255);
ALTER TABLE users ADD COLUMN email_verification_expires_at TIMESTAMP;
ALTER TABLE users ADD COLUMN password_reset_token VARCHAR(255);
ALTER TABLE users ADD COLUMN password_reset_expires_at TIMESTAMP;

CREATE INDEX idx_users_email_verification_token ON users(email_verification_token);
CREATE INDEX idx_users_password_reset_token ON users(password_reset_token);
