-- liquibase formatted sql
-- changeset station-service:002-create-slots

CREATE TABLE IF NOT EXISTS slots (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    station_id  UUID        NOT NULL REFERENCES stations(id),
    slot_number INTEGER     NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'FREE',
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (station_id, slot_number)
);

-- Index for fetching all slots in a station (frequent operation)
CREATE INDEX IF NOT EXISTS idx_slots_station_id ON slots(station_id);

-- Index to quickly count FREE slots per station
CREATE INDEX IF NOT EXISTS idx_slots_status ON slots(status);

-- rollback DROP TABLE IF EXISTS slots;
