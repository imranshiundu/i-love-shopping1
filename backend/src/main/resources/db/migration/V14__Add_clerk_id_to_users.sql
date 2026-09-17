-- Links store accounts to Clerk identities for social sign-in.
ALTER TABLE users ADD COLUMN IF NOT EXISTS clerk_id VARCHAR(255);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_clerk_id ON users(clerk_id);
