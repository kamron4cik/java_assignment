-- liquibase formatted sql
-- changeset payment-service:002-create-payments

CREATE TABLE IF NOT EXISTS payments (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID         NOT NULL,
    card_id         UUID         NOT NULL REFERENCES cards(id),
    rental_id       UUID,
    amount          NUMERIC(19, 2) NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    -- idempotency_key UNIQUE prevents duplicate payments on client retries
    idempotency_key VARCHAR(100) NOT NULL UNIQUE,
    failure_reason  TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ
);

-- Index for user payment history queries
CREATE INDEX IF NOT EXISTS idx_payments_user_id ON payments(user_id);

-- Index for looking up payments by rental (recurring billing)
CREATE INDEX IF NOT EXISTS idx_payments_rental_id ON payments(rental_id);

-- Index for filtering by status (e.g. find all PENDING payments)
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);

-- UNIQUE index on idempotency_key (already covered by UNIQUE constraint above)
-- but explicit for clarity and documentation
CREATE UNIQUE INDEX IF NOT EXISTS idx_payments_idempotency_key ON payments(idempotency_key);

-- rollback DROP TABLE IF EXISTS payments;
