-- Table principale : une ligne par utilisateur
CREATE TABLE user_reroll (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL UNIQUE REFERENCES ts_user(id),
    CONSTRAINT fk_user_reroll_user FOREIGN KEY (user_id) REFERENCES ts_user(id)
);

-- Cooldowns par rareté (max 4 lignes par user_reroll)
CREATE TABLE reroll_cooldown (
    id              BIGSERIAL PRIMARY KEY,
    user_reroll_id  BIGINT NOT NULL REFERENCES user_reroll(id) ON DELETE CASCADE,
    rarity          VARCHAR(20) NOT NULL,
    last_reroll_at  TIMESTAMP NOT NULL,
    CONSTRAINT fk_reroll_cooldown_user_reroll FOREIGN KEY (user_reroll_id) REFERENCES user_reroll(id) ON DELETE CASCADE,
    CONSTRAINT uq_reroll_cooldown_rarity UNIQUE (user_reroll_id, rarity)
);
