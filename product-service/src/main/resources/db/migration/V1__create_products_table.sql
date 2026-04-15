-- V1: creates the products table
-- Phase 4 Part 1: basic product catalog with in-stock quantity.
-- The quantity_in_stock column will be migrated to a separate inventory
-- table in Phase 4 Part 2 when inventory is modelled as its own aggregate.

CREATE TABLE products
(
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    name              VARCHAR(255) NOT NULL,
    description       VARCHAR(1000),
    price             NUMERIC(10, 2) NOT NULL,
    sku               VARCHAR(100) NOT NULL,
    quantity_in_stock INTEGER      NOT NULL DEFAULT 0,
    created_at        TIMESTAMP    NOT NULL,
    updated_at        TIMESTAMP    NOT NULL,

    CONSTRAINT pk_products PRIMARY KEY (id),
    CONSTRAINT uq_products_sku UNIQUE (sku),
    CONSTRAINT ck_products_price CHECK (price >= 0),
    CONSTRAINT ck_products_quantity CHECK (quantity_in_stock >= 0)
);
