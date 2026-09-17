-- Widens assignable roles (admin, support, sales, moderator).
ALTER TABLE user_roles DROP CONSTRAINT IF EXISTS user_roles_role_check;
ALTER TABLE user_roles DROP CONSTRAINT IF EXISTS chk_user_roles_role;
DO $$
DECLARE constraint_name TEXT;
BEGIN
    SELECT c.conname INTO constraint_name FROM pg_constraint c
    JOIN pg_class t ON c.conrelid = t.oid
    WHERE t.relname = 'user_roles' AND c.contype = 'c' LIMIT 1;
    IF constraint_name IS NOT NULL THEN
        EXECUTE format('ALTER TABLE user_roles DROP CONSTRAINT %I', constraint_name);
    END IF;
END $$;
ALTER TABLE user_roles ADD CONSTRAINT user_roles_role_check
    CHECK (role IN ('USER', 'ADMIN', 'MODERATOR', 'SUPPORT', 'SALES'));
