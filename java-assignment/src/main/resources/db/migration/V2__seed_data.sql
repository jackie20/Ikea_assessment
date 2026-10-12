-- Seed stores
INSERT INTO store (id, name, quantityproductsinstock) VALUES
    (1, 'TONSTAD', 10),
    (2, 'KALLAX',  5),
    (3, 'BESTÅ',   3)
ON CONFLICT (name) DO NOTHING;
SELECT SETVAL('store_seq', 4, false);

-- Seed products
INSERT INTO product (id, name, stock) VALUES
    (1, 'TONSTAD', 10),
    (2, 'KALLAX',  5),
    (3, 'BESTÅ',   3)
ON CONFLICT (name) DO NOTHING;
SELECT SETVAL('product_seq', 4, false);

-- Seed warehouses
INSERT INTO warehouse (id, businessunitcode, location, capacity, stock, createdat, archivedat) VALUES
    (1, 'MWH.001', 'ZWOLLE-001',    30, 10, '2024-07-01 00:00:00', NULL),
    (2, 'MWH.012', 'AMSTERDAM-001', 50,  5, '2023-07-01 00:00:00', NULL),
    (3, 'MWH.023', 'TILBURG-001',   30, 27, '2021-02-01 00:00:00', NULL)
ON CONFLICT (businessunitcode) DO NOTHING;
SELECT SETVAL('warehouse_seq', 4, false);
