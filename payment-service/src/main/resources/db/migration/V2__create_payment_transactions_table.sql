CREATE TABLE payment_transactions
(
    id         UUID           NOT NULL DEFAULT gen_random_uuid(),
    payment_id UUID           NOT NULL,
    order_id   UUID           NOT NULL,
    amount     NUMERIC(12, 2) NOT NULL,
    status     VARCHAR(20)    NOT NULL,
    created_at TIMESTAMP      NOT NULL,

    CONSTRAINT pk_payment_transactions PRIMARY KEY (id),
    CONSTRAINT fk_payment_transactions_payment FOREIGN KEY (payment_id) REFERENCES payments (id)
);

CREATE INDEX idx_payment_transactions_payment_id ON payment_transactions (payment_id);
CREATE INDEX idx_payment_transactions_order_id ON payment_transactions (order_id);
