-- ==============================
--   SEQUENCES
-- ==============================
CREATE SEQUENCE IF NOT EXISTS card_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS live_feed_event_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS pack_slot_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS pack_template_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS pack_template_slot_seq START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS ts_user_seq START WITH 1 INCREMENT BY 50;

-- ==============================
--   TABLES
-- ==============================

CREATE TABLE ts_user (
    id          BIGINT PRIMARY KEY,
    keycloak_id VARCHAR(255) NOT NULL UNIQUE,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE card (
    id             BIGINT PRIMARY KEY,
    name           VARCHAR(255),
    image_url      VARCHAR(512),
    rarity         VARCHAR(255) NOT NULL,
    description    VARCHAR(255),
    drop_rate      DOUBLE PRECISION NOT NULL,
    is_unique      BOOLEAN NOT NULL,
    creator_id     BIGINT REFERENCES ts_user(id),
    target_user_id BIGINT REFERENCES ts_user(id)
);

CREATE TABLE user_cards (
    user_id BIGINT NOT NULL REFERENCES ts_user(id),
    card_id BIGINT NOT NULL REFERENCES card(id),
    PRIMARY KEY (user_id, card_id)
);

CREATE TABLE pack_slot (
    id           BIGINT PRIMARY KEY,
    name         VARCHAR(255) UNIQUE,
    fixed_rarity VARCHAR(255)
);

CREATE TABLE pack_slot_rarity_weights (
    pack_slot_id BIGINT NOT NULL REFERENCES pack_slot(id),
    rarity       VARCHAR(255) NOT NULL,
    weight       DOUBLE PRECISION,
    PRIMARY KEY (pack_slot_id, rarity)
);

CREATE TABLE pack_template (
    id   BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE pack_template_slot (
    id               BIGINT PRIMARY KEY,
    pack_template_id BIGINT NOT NULL REFERENCES pack_template(id),
    pack_slot_id     BIGINT NOT NULL REFERENCES pack_slot(id),
    count            INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE live_feed_event (
    id              BIGINT PRIMARY KEY,
    actor_username  VARCHAR(255) NOT NULL,
    card_name       VARCHAR(255) NOT NULL,
    card_rarity     VARCHAR(255) NOT NULL,
    target_username VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP NOT NULL
);
