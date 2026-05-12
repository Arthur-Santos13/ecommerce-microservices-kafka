ALTER TABLE payments
    ADD COLUMN external_transaction_id VARCHAR(128),
    ADD COLUMN payment_instructions TEXT;
