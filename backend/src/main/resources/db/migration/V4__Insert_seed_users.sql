-- V4__Insert_seed_users.sql
-- Seed users for local development and testing.
-- Passwords (both hashed with BCrypt cost 12):
--   admin@iloveshopping.com  / Admin123!
--   user@iloveshopping.com   / User123!

INSERT INTO users (id, email, password_hash, name, email_verified, two_factor_enabled, created_at, updated_at) VALUES
('e1000001-0000-4000-8000-000000000001', 'admin@iloveshopping.com', '$2a$12$gATcDEh3pQzMHSJgyEWdieFtgVv8nNLq1oOhaXCskHyuVrn9Emvya', 'Admin User', CURRENT_TIMESTAMP, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('e1000001-0000-4000-8000-000000000002', 'user@iloveshopping.com', '$2a$12$7yfRsKszw6rUbBYT8odU/.IqQDTSrQXZZofwxorxU/WzQvRRCCvhO', 'Test User', CURRENT_TIMESTAMP, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO user_roles (user_id, role) VALUES
('e1000001-0000-4000-8000-000000000001', 'ADMIN'),
('e1000001-0000-4000-8000-000000000002', 'USER');