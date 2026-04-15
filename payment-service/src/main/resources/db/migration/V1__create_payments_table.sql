CREATE TABLE payments
(
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id       UUID          NOT NULL UNIQUE,
    customer_id    UUID          NOT NULL,
    amount         NUMERIC(12, 2) NOT NULL,
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
    method         VARCHAR(20)   NOT NULL,
    failure_reason VARCHAR(255),
    created_at     TIMESTAMP     NOT NULL,
    updated_at     TIMESTAMP     NOT NULL
);

CREATE INDEX idx_payments_order_id ON payments (order_id);
CREATE INDEX idx_payments_customer_id ON payments (customer_id);
CREATE INDEX idx_payments_status ON payments (status);
