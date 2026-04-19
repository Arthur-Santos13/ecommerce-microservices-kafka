-- V7: seed mock data for local development and integration testing
-- Categories and products with inventory for visual validation via the frontend.

INSERT INTO categories (id, name, description, created_at, updated_at)
VALUES
    ('a1b2c3d4-0001-0000-0000-000000000001', 'Eletrônicos',  'Smartphones, notebooks e acessórios', NOW(), NOW()),
    ('a1b2c3d4-0002-0000-0000-000000000002', 'Periféricos',  'Teclados, mouses e monitores',        NOW(), NOW()),
    ('a1b2c3d4-0003-0000-0000-000000000003', 'Áudio',        'Fones de ouvido e caixas de som',     NOW(), NOW()),
    ('a1b2c3d4-0004-0000-0000-000000000004', 'Acessórios',   'Cabos, adaptadores e suportes',       NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO products (id, name, description, price, sku, category_id, created_at, updated_at)
VALUES
    ('b1c2d3e4-0001-0000-0000-000000000001',
     'Notebook Pro 16',
     'Processador Intel Core i7, 16 GB RAM, SSD 512 GB, tela 16"',
     4999.90, 'NB-PRO-16',
     'a1b2c3d4-0001-0000-0000-000000000001',
     NOW(), NOW()),

    ('b1c2d3e4-0002-0000-0000-000000000002',
     'Smartphone Ultra X',
     'Tela AMOLED 6.7", 256 GB, câmera 108 MP, bateria 5000 mAh',
     3499.00, 'SM-ULTRA-X',
     'a1b2c3d4-0001-0000-0000-000000000001',
     NOW(), NOW()),

    ('b1c2d3e4-0003-0000-0000-000000000003',
     'Teclado Mecânico RGB',
     'Switch Cherry MX Red, layout ABNT2, retroiluminação RGB',
     599.90, 'KB-MEC-RGB',
     'a1b2c3d4-0002-0000-0000-000000000002',
     NOW(), NOW()),

    ('b1c2d3e4-0004-0000-0000-000000000004',
     'Mouse Gamer 16000 DPI',
     'Sensor óptico, 7 botões programáveis, polling rate 1000 Hz',
     299.90, 'MS-GAMER-16K',
     'a1b2c3d4-0002-0000-0000-000000000002',
     NOW(), NOW()),

    ('b1c2d3e4-0005-0000-0000-000000000005',
     'Monitor IPS 27" 144Hz',
     'Resolução QHD 2560x1440, 1ms GTG, HDR400, FreeSync Premium',
     1899.00, 'MN-IPS-27-144',
     'a1b2c3d4-0002-0000-0000-000000000002',
     NOW(), NOW()),

    ('b1c2d3e4-0006-0000-0000-000000000006',
     'Fone Over-Ear ANC',
     'Cancelamento ativo de ruído, Bluetooth 5.3, 30h de bateria',
     899.90, 'HP-ANC-OE',
     'a1b2c3d4-0003-0000-0000-000000000003',
     NOW(), NOW()),

    ('b1c2d3e4-0007-0000-0000-000000000007',
     'Caixa de Som Bluetooth 40W',
     'Resistente à água IPX5, 12h de bateria, conexão dupla',
     449.90, 'SP-BT-40W',
     'a1b2c3d4-0003-0000-0000-000000000003',
     NOW(), NOW()),

    ('b1c2d3e4-0008-0000-0000-000000000008',
     'Cabo USB-C para USB-C 2m',
     'Carregamento rápido 100W PD, transferência 10 Gbps, trançado',
     79.90, 'CB-USBC-2M',
     'a1b2c3d4-0004-0000-0000-000000000004',
     NOW(), NOW()),

    ('b1c2d3e4-0009-0000-0000-000000000009',
     'Hub USB-C 7 em 1',
     'HDMI 4K, 2x USB-A 3.0, USB-C PD 100W, SD/microSD, RJ45',
     189.90, 'HB-USBC-7IN1',
     'a1b2c3d4-0004-0000-0000-000000000004',
     NOW(), NOW()),

    ('b1c2d3e4-0010-0000-0000-000000000010',
     'SSD Externo 1TB',
     'USB 3.2 Gen 2, leitura 1050 MB/s, compacto e resistente',
     549.90, 'SSD-EXT-1TB',
     'a1b2c3d4-0001-0000-0000-000000000001',
     NOW(), NOW())
ON CONFLICT DO NOTHING;

INSERT INTO inventory (id, product_id, quantity_in_stock, reserved_quantity, updated_at)
VALUES
    ('c1d2e3f4-0001-0000-0000-000000000001', 'b1c2d3e4-0001-0000-0000-000000000001', 25,  0, NOW()),
    ('c1d2e3f4-0002-0000-0000-000000000002', 'b1c2d3e4-0002-0000-0000-000000000002', 40,  0, NOW()),
    ('c1d2e3f4-0003-0000-0000-000000000003', 'b1c2d3e4-0003-0000-0000-000000000003', 60,  0, NOW()),
    ('c1d2e3f4-0004-0000-0000-000000000004', 'b1c2d3e4-0004-0000-0000-000000000004', 75,  0, NOW()),
    ('c1d2e3f4-0005-0000-0000-000000000005', 'b1c2d3e4-0005-0000-0000-000000000005', 15,  0, NOW()),
    ('c1d2e3f4-0006-0000-0000-000000000006', 'b1c2d3e4-0006-0000-0000-000000000006', 30,  0, NOW()),
    ('c1d2e3f4-0007-0000-0000-000000000007', 'b1c2d3e4-0007-0000-0000-000000000007', 50,  0, NOW()),
    ('c1d2e3f4-0008-0000-0000-000000000008', 'b1c2d3e4-0008-0000-0000-000000000008', 200, 0, NOW()),
    ('c1d2e3f4-0009-0000-0000-000000000009', 'b1c2d3e4-0009-0000-0000-000000000009', 80,  0, NOW()),
    ('c1d2e3f4-0010-0000-0000-000000000010', 'b1c2d3e4-0010-0000-0000-000000000010', 35,  0, NOW())
ON CONFLICT DO NOTHING;
