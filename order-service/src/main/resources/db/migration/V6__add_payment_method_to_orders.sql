ALTER TABLE orders
    ADD COLUMN payment_method VARCHAR(20) NOT NULL DEFAULT 'CREDIT_CARD';

CREATE INDEX idx_orders_payment_method ON orders (payment_method);
