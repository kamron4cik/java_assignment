-- liquibase formatted sql
-- changeset station-service:001-create-stations

CREATE TABLE IF NOT EXISTS stations (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(255) NOT NULL,
    latitude   DOUBLE PRECISION NOT NULL,
    longitude  DOUBLE PRECISION NOT NULL,
    address    VARCHAR(500) NOT NULL,
    is_active  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ
);

-- Composite index on lat/lon for geospatial distance queries
-- (Haversine formula scans all rows; for production use PostGIS instead)
CREATE INDEX IF NOT EXISTS idx_stations_location ON stations(latitude, longitude);

-- rollback DROP TABLE IF EXISTS stations;
