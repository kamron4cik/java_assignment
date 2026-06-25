-- liquibase formatted sql
-- changeset payment-service:001-create-cards

CREATE TABLE IF NOT EXISTS cards (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID         NOT NULL,
    card_number VARCHAR(20)  NOT NULL,
    card_holder VARCHAR(255) NOT NULL,
    -- NUMERIC not DOUBLE — floating point is never acceptable for money
    balance     NUMERIC(19, 2) NOT NULL DEFAULT 0.00,
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ
);

-- Index for fetching all cards belonging to a user (most frequent query)
CREATE INDEX IF NOT EXISTS idx_cards_user_id ON cards(user_id);

-- Index on card_number for uniqueness checks
CREATE INDEX IF NOT EXISTS idx_cards_card_number ON cards(card_number);

-- rollback DROP TABLE IF EXISTS cards;
