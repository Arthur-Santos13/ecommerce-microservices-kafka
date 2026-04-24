-- V5: fix seed data with invalid status 'PAID' (not in OrderStatus enum)
-- 'PAID' semantically corresponds to 'CONFIRMED' in this domain model
UPDATE orders SET status = 'CONFIRMED' WHERE status = 'PAID';
