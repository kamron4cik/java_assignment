-- liquibase formatted sql
-- changeset user-service:001-create-users

CREATE TABLE IF NOT EXISTS users (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone       VARCHAR(20)  NOT NULL UNIQUE,
    keycloak_id VARCHAR(255) UNIQUE,
    full_name   VARCHAR(255),
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ
);

-- Index on phone for fast lookup by phone number during auth
CREATE INDEX IF NOT EXISTS idx_users_phone ON users(phone);

-- Index on keycloak_id for JWT subject lookup
CREATE INDEX IF NOT EXISTS idx_users_keycloak_id ON users(keycloak_id);

-- rollback DROP TABLE IF EXISTS users;
