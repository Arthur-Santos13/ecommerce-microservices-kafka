-- V2: Extract inventory into its own table.
--
-- Rationale: Inventory is a separable aggregate (see Inventory.java).
-- Keeping it in a dedicated table enables:
--   - Independent scaling of stock-related writes
--   - Cleaner schema for future extraction into inventory-service
--   - reserved_quantity support (needed for order reservation flow, Phase 5+)
--
-- Steps:
--   1. Create inventory table with product_id FK and check constraints
--   2. Migrate existing quantity_in_stock data
--   3. Drop quantity_in_stock from products

CREATE TABLE inventory
(
    id                UUID    NOT NULL DEFAULT gen_random_uuid(),
    product_id        UUID    NOT NULL,
    quantity_in_stock INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    updated_at        TIMESTAMP NOT NULL,

    CONSTRAINT pk_inventory PRIMARY KEY (id),
    CONSTRAINT fk_inventory_product
        FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE,
    CONSTRAINT uq_inventory_product UNIQUE (product_id),
    CONSTRAINT ck_inventory_quantity CHECK (quantity_in_stock >= 0),
    CONSTRAINT ck_inventory_reserved CHECK (reserved_quantity >= 0),
    CONSTRAINT ck_inventory_reserved_lte_stock CHECK (reserved_quantity <= quantity_in_stock)
);

INSERT INTO inventory (id, product_id, quantity_in_stock, reserved_quantity, updated_at)
SELECT gen_random_uuid(), id, quantity_in_stock, 0, NOW()
FROM products;

ALTER TABLE products
    DROP COLUMN quantity_in_stock;
