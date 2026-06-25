-- liquibase formatted sql
-- changeset station-service:003-create-powerbanks

CREATE TABLE IF NOT EXISTS power_banks (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    slot_id       UUID        REFERENCES slots(id),
    battery_level INTEGER     NOT NULL DEFAULT 100,
    status        VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ
);

-- Index to find AVAILABLE powerbanks quickly (for dispensing)
CREATE INDEX IF NOT EXISTS idx_powerbank_status ON power_banks(status);

-- rollback DROP TABLE IF EXISTS power_banks;
