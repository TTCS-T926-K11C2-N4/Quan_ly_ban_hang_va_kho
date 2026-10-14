-- =====================================================================
-- S5-05: tồn kho đầu kỳ mẫu để dựng và thử màn Sổ tồn kho (đủ ba trạng thái Hết hàng / Sắp hết / Còn hàng).
-- Chạy SAU sample_products.sql và sample_warehouses_regions.sql.
-- Mỗi dòng tồn có thẻ kho (stock_movements, ref_type OPENING_BALANCE) và tồn theo lô mặc định của SKU, như khi
-- nhập kho thật. Chạy lại lần 2 không lỗi và không cộng dồn: (kho, SKU) đã có dòng tồn thì bỏ qua.
-- sample_reservations.sql (S5-06) dùng lại đúng các dòng tồn này cho phần SKU nó giữ chỗ.
-- =====================================================================

SET NAMES utf8mb4;

DROP TEMPORARY TABLE IF EXISTS sample_opening_stock;
CREATE TEMPORARY TABLE sample_opening_stock (
  warehouse_code varchar(30), sku varchar(50), qty decimal(18,3), min_stock decimal(18,3)
);
INSERT INTO sample_opening_stock VALUES
  ('KHO-TT', 'SP001', 480, 120), ('KHO-TT', 'SP002', 240, 120), ('KHO-TT', 'SP003', 96, 120),
  ('KHO-TT', 'SP004', 200, 0),   ('KHO-TT', 'SP006', 360, 100), ('KHO-TT', 'SP007', 0, 50),
  ('KHO-TT', 'SP011', 600, 200), ('KHO-TT', 'SP012', 144, 60),  ('KHO-TT', 'SP013', 36, 24),
  ('KHO-TT', 'SP014', 15, 48),   ('KHO-TT', 'SP015', 120, 0),   ('KHO-TT', 'SP018', 60, 24),
  ('KHO-TT', 'SP019', 0, 12),    ('KHO-TT', 'SP021', 85, 0),
  ('KHO-HN', 'SP001', 240, 96),  ('KHO-HN', 'SP006', 48, 96),   ('KHO-HN', 'SP009', 192, 0),
  ('KHO-HN', 'SP011', 300, 120), ('KHO-HN', 'SP017', 250, 0),   ('KHO-HN', 'SP020', 0, 24),
  ('KHO-BN', 'SP002', 120, 0),   ('KHO-BN', 'SP010', 72, 72),   ('KHO-BN', 'SP021', 85, 0),
  ('KHO-LOI', 'SP011', 24, 0),   ('KHO-LOI', 'SP013', 12, 0);

-- Lô mặc định cho SKU không quản lý lô (cùng quy ước với phiếu nhập kho S5-04)
INSERT IGNORE INTO `lots` (`product_id`, `lot_no`, `is_default`, `created_at`)
SELECT DISTINCT p.id, 'MAC-DINH', true, UTC_TIMESTAMP()
FROM sample_opening_stock s JOIN `products` p ON p.sku = s.sku
WHERE NOT EXISTS (SELECT 1 FROM `lots` l WHERE l.product_id = p.id AND l.is_default);

-- Thẻ kho trước, chỉ cho (kho, SKU) chưa có dòng tồn
INSERT INTO `stock_movements` (`warehouse_id`, `product_id`, `lot_id`, `movement_type`, `qty_change_base`,
                               `balance_after_base`, `ref_type`, `ref_id`, `occurred_at`, `note`, `created_at`)
SELECT w.id, p.id, l.id, 'ADJUSTMENT', s.qty, s.qty, 'OPENING_BALANCE', 0, UTC_TIMESTAMP(), 'Tồn đầu kỳ (dữ liệu mẫu)',
       UTC_TIMESTAMP()
FROM sample_opening_stock s
JOIN `warehouses` w ON w.code = s.warehouse_code
JOIN `products` p ON p.sku = s.sku
JOIN `lots` l ON l.product_id = p.id AND l.is_default
WHERE s.qty > 0
  AND NOT EXISTS (SELECT 1 FROM `stock_balances` b WHERE b.warehouse_id = w.id AND b.product_id = p.id);

INSERT IGNORE INTO `stock_lot_balances` (`warehouse_id`, `lot_id`, `qty_on_hand_base`, `updated_at`)
SELECT w.id, l.id, s.qty, UTC_TIMESTAMP()
FROM sample_opening_stock s
JOIN `warehouses` w ON w.code = s.warehouse_code
JOIN `products` p ON p.sku = s.sku
JOIN `lots` l ON l.product_id = p.id AND l.is_default
WHERE s.qty > 0
  AND NOT EXISTS (SELECT 1 FROM `stock_balances` b WHERE b.warehouse_id = w.id AND b.product_id = p.id);

INSERT IGNORE INTO `stock_balances` (`warehouse_id`, `product_id`, `min_stock_base`, `qty_on_hand_base`, `updated_at`)
SELECT w.id, p.id, s.min_stock, s.qty, UTC_TIMESTAMP()
FROM sample_opening_stock s
JOIN `warehouses` w ON w.code = s.warehouse_code
JOIN `products` p ON p.sku = s.sku;

DROP TEMPORARY TABLE sample_opening_stock;
