-- liquibase formatted sql
-- changeset user-service:002-create-otp-codes

CREATE TABLE IF NOT EXISTS otp_codes (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    phone      VARCHAR(20)  NOT NULL,
    code       VARCHAR(10)  NOT NULL,
    is_used    BOOLEAN      NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMPTZ  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Index on phone to quickly look up OTPs by phone number
CREATE INDEX IF NOT EXISTS idx_otp_phone ON otp_codes(phone);

-- Index on expires_at for efficient cleanup of expired OTPs
CREATE INDEX IF NOT EXISTS idx_otp_expires_at ON otp_codes(expires_at);

-- rollback DROP TABLE IF EXISTS otp_codes;
