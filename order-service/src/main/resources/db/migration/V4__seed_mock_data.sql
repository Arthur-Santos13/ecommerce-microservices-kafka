-- V4: seed mock orders for local development and integration testing

INSERT INTO orders (id, customer_id, status, total_amount, created_at, updated_at)
VALUES
    ('d1e2f3a4-0001-0000-0000-000000000001',
     'e1f2a3b4-0001-0000-0000-000000000001',
     'CONFIRMED', 4999.90,
     NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),

    ('d1e2f3a4-0002-0000-0000-000000000002',
     'e1f2a3b4-0001-0000-0000-000000000001',
     'AWAITING_PAYMENT', 899.80,
     NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),

    ('d1e2f3a4-0003-0000-0000-000000000003',
     'e1f2a3b4-0002-0000-0000-000000000002',
     'CONFIRMED', 3498.80,
     NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),

    ('d1e2f3a4-0004-0000-0000-000000000004',
     'e1f2a3b4-0002-0000-0000-000000000002',
     'CANCELLED', 549.90,
     NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),

    ('d1e2f3a4-0005-0000-0000-000000000005',
     'e1f2a3b4-0003-0000-0000-000000000003',
     'AWAITING_PAYMENT', 1189.70,
     NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO order_items (id, order_id, product_id, product_name, unit_price, quantity, subtotal)
VALUES
    -- Order 1: Notebook Pro 16
    ('f1a2b3c4-0001-0000-0000-000000000001',
     'd1e2f3a4-0001-0000-0000-000000000001',
     'b1c2d3e4-0001-0000-0000-000000000001',
     'Notebook Pro 16', 4999.90, 1, 4999.90),

    -- Order 2: Teclado + Mouse
    ('f1a2b3c4-0002-0000-0000-000000000002',
     'd1e2f3a4-0002-0000-0000-000000000002',
     'b1c2d3e4-0003-0000-0000-000000000003',
     'Teclado Mecânico RGB', 599.90, 1, 599.90),

    ('f1a2b3c4-0003-0000-0000-000000000003',
     'd1e2f3a4-0002-0000-0000-000000000002',
     'b1c2d3e4-0004-0000-0000-000000000004',
     'Mouse Gamer 16000 DPI', 299.90, 1, 299.90),

    -- Order 3: Smartphone + Fone
    ('f1a2b3c4-0004-0000-0000-000000000004',
     'd1e2f3a4-0003-0000-0000-000000000003',
     'b1c2d3e4-0002-0000-0000-000000000002',
     'Smartphone Ultra X', 3499.00, 1, 3499.00),

    ('f1a2b3c4-0005-0000-0000-000000000005',
     'd1e2f3a4-0003-0000-0000-000000000003',
     'b1c2d3e4-0008-0000-0000-000000000008',
     'Cabo USB-C para USB-C 2m', 79.90, 1, 79.90),

    -- Order 4 (cancelado): SSD Externo
    ('f1a2b3c4-0006-0000-0000-000000000006',
     'd1e2f3a4-0004-0000-0000-000000000004',
     'b1c2d3e4-0010-0000-0000-000000000010',
     'SSD Externo 1TB', 549.90, 1, 549.90),

    -- Order 5: Caixa de Som + Hub + Cabo
    ('f1a2b3c4-0007-0000-0000-000000000007',
     'd1e2f3a4-0005-0000-0000-000000000005',
     'b1c2d3e4-0007-0000-0000-000000000007',
     'Caixa de Som Bluetooth 40W', 449.90, 1, 449.90),

    ('f1a2b3c4-0008-0000-0000-000000000008',
     'd1e2f3a4-0005-0000-0000-000000000005',
     'b1c2d3e4-0009-0000-0000-000000000009',
     'Hub USB-C 7 em 1', 189.90, 1, 189.90),

    ('f1a2b3c4-0009-0000-0000-000000000009',
     'd1e2f3a4-0005-0000-0000-000000000005',
     'b1c2d3e4-0008-0000-0000-000000000008',
     'Cabo USB-C para USB-C 2m', 79.90, 1, 79.90)
ON CONFLICT DO NOTHING;
