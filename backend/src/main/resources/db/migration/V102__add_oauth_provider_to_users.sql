-- ========================================================
-- SlopeGuard Database Migration: V102__add_oauth_provider_to_users.sql
-- Module 1 & Security: Support Google OAuth2 / OpenID Connect
-- ========================================================

-- 1. Modify password_hash to allow NULL for OAuth accounts (passwords disabled until explicitly set)
ALTER TABLE users MODIFY COLUMN password_hash VARCHAR(255) NULL;

-- 2. Add auth_provider, provider_id, and profile_picture_url
ALTER TABLE users ADD COLUMN auth_provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE users ADD COLUMN provider_id VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN profile_picture_url VARCHAR(512) NULL;

-- 3. Composite unique constraint for provider and provider_id (prevents duplicate OAuth links)
ALTER TABLE users ADD CONSTRAINT uk_users_provider UNIQUE (auth_provider, provider_id);

-- 4. Index on provider columns for fast lookup
CREATE INDEX idx_users_provider ON users (auth_provider, provider_id);

