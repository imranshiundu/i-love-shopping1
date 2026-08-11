-- V5__Drop_unused_refresh_tokens_table.sql
-- The refresh token mechanism is handled by the sessions table.
-- This legacy table has no mapped entity and is no longer used.

DROP TABLE IF EXISTS refresh_tokens;
