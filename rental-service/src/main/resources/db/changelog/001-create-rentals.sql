-- liquibase formatted sql
-- changeset rental-service:001-create-rentals

CREATE TABLE IF NOT EXISTS rentals (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id          UUID          NOT NULL,
    station_id       UUID          NOT NULL,
    card_id          UUID          NOT NULL,
    slot_id          UUID,
    power_bank_id    UUID,
    status           VARCHAR(30)   NOT NULL DEFAULT 'WAITING',
    -- idempotency_key UNIQUE — prevents duplicate rentals on client retries
    idempotency_key  VARCHAR(100)  NOT NULL UNIQUE,
    failure_reason   TEXT,
    rate_per_minute  NUMERIC(10,4) NOT NULL DEFAULT 0.0500,
    total_amount     NUMERIC(19,2),
    started_at       TIMESTAMPTZ,
    finished_at      TIMESTAMPTZ,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ
);

-- Index for fetching user's rental history (common query)
CREATE INDEX IF NOT EXISTS idx_rentals_user_id ON rentals(user_id);

-- Index for FSM polling: find all rentals in a specific state
CREATE INDEX IF NOT EXISTS idx_rentals_status ON rentals(status);

-- UNIQUE index on idempotency_key (also covered by UNIQUE constraint)
CREATE UNIQUE INDEX IF NOT EXISTS idx_rentals_idempotency_key ON rentals(idempotency_key);

-- Index for station queries
CREATE INDEX IF NOT EXISTS idx_rentals_station_id ON rentals(station_id);

-- rollback DROP TABLE IF EXISTS rentals;
