-- Seeds inventory_service.inventory with 1000 rows, sku_code SKU-00001..SKU-01000,
-- matching the SKUs seed-products.js inserts into product_service.products.
-- Every 50th SKU starts at zero stock, to exercise the 409 insufficient-stock path
-- in order-service without hand-picking specific SKUs.
-- Run: docker exec -i postgres-inventory psql -U root -d inventory_service < scripts/seed-inventory.sql

INSERT INTO inventory (id, sku_code, quantity, created_at, updated_at)
SELECT
  gen_random_uuid()::varchar(36),
  'SKU-' || lpad(gs::text, 5, '0'),
  CASE WHEN gs % 50 = 0 THEN 0 ELSE floor(random() * 500)::int END,
  now(),
  now()
FROM generate_series(1, 1000) AS gs
ON CONFLICT (sku_code) DO NOTHING;
