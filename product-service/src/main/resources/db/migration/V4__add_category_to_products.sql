-- V4: add category relationship to products
-- category_id is nullable — products do not require a category at creation.

ALTER TABLE products
    ADD COLUMN category_id UUID;

ALTER TABLE products
    ADD CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL;
