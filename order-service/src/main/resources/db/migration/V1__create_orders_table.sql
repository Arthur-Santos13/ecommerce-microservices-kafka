CREATE TABLE orders
(
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id  UUID         NOT NULL,
    status       VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    total_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL,
    updated_at   TIMESTAMP    NOT NULL
);

CREATE TABLE order_items
(
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id     UUID          NOT NULL REFERENCES orders (id) ON DELETE CASCADE,
    product_id   UUID          NOT NULL,
    product_name VARCHAR(255)  NOT NULL,
    unit_price   NUMERIC(10, 2) NOT NULL,
    quantity     INT           NOT NULL CHECK (quantity > 0),
    subtotal     NUMERIC(12, 2) NOT NULL
);

CREATE INDEX idx_orders_customer_id ON orders (customer_id);
CREATE INDEX idx_order_items_order_id ON order_items (order_id);
