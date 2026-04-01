CREATE TABLE dodge_score (
    id         BIGSERIAL    PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES ts_user(id) ON DELETE CASCADE,
    best_score INT          NOT NULL DEFAULT 0,
    week_start DATE         NOT NULL,
    UNIQUE (user_id, week_start)
);

CREATE INDEX idx_dodge_score_week ON dodge_score (week_start, best_score DESC);
