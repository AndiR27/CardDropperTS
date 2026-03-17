CREATE TABLE trade_session (
    id              UUID PRIMARY KEY,
    session_status  VARCHAR(20)  NOT NULL,
    initiator_id    BIGINT       NOT NULL,
    receiver_id     BIGINT,
    initiator_card_id BIGINT,
    receiver_card_id  BIGINT,
    initiator_locked  BOOLEAN    NOT NULL DEFAULT FALSE,
    receiver_locked   BOOLEAN    NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP    NOT NULL,
    completed_at    TIMESTAMP,

    CONSTRAINT fk_trade_initiator    FOREIGN KEY (initiator_id)     REFERENCES ts_user(id),
    CONSTRAINT fk_trade_receiver     FOREIGN KEY (receiver_id)      REFERENCES ts_user(id),
    CONSTRAINT fk_trade_init_card    FOREIGN KEY (initiator_card_id) REFERENCES card(id),
    CONSTRAINT fk_trade_recv_card    FOREIGN KEY (receiver_card_id)  REFERENCES card(id)
);
