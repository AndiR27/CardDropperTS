-- ===========================================
--   V5 : Allow a user to own the same card multiple times
--   Replace composite PK (user_id, card_id) with a surrogate id
-- ===========================================

ALTER TABLE user_cards DROP CONSTRAINT user_cards_pkey;

CREATE SEQUENCE user_cards_seq START WITH 1 INCREMENT BY 1;

ALTER TABLE user_cards
    ADD COLUMN id BIGINT NOT NULL DEFAULT nextval('user_cards_seq');

ALTER TABLE user_cards ADD PRIMARY KEY (id);

CREATE INDEX idx_user_cards_user_id ON user_cards (user_id);
CREATE INDEX idx_user_cards_card_id ON user_cards (card_id);
