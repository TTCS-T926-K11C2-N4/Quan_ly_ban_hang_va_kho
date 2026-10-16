-- =====================================================================
-- Sprint 6 (S6-03 soạn hàng, S6-05 xuất kho, S6-08 giao hàng): dữ liệu mẫu để thử luồng soạn - xuất - giao.
--  * Gán vị trí lưu chính (stock_balances.location_id) cho các SKU ở KHO-TT, KHO-HN theo cây vị trí của
--    sample_warehouse_locations.sql, để dòng soạn có vị trí và sắp theo đường đi.
--  * Hẹn ngày giao cho các đơn mẫu đã duyệt (sample_reservations.sql) chưa có ngày giao: hôm nay và ngày mai.
--  * Thêm một lô có hạn sử dụng cho SP001 ở KHO-TT (tách từ lô mặc định, tổng tồn không đổi) để thấy chọn lô FEFO.
-- Chạy SAU sample_warehouse_locations.sql, sample_stock.sql, sample_reservations.sql. Chạy lại không lỗi, không trùng.
-- =====================================================================

SET NAMES utf8mb4;

DROP TEMPORARY TABLE IF EXISTS sample_sku_location;
CREATE TEMPORARY TABLE sample_sku_location (warehouse_code varchar(30), sku varchar(50), location_code varchar(30));
INSERT INTO sample_sku_location VALUES
  ('KHO-TT', 'SP001', 'A-01'), ('KHO-TT', 'SP002', 'A-01'), ('KHO-TT', 'SP003', 'A-01'),
  ('KHO-TT', 'SP004', 'A-02'), ('KHO-TT', 'SP006', 'A-03'), ('KHO-TT', 'SP007', 'A-03'),
  ('KHO-TT', 'SP011', 'A-03'), ('KHO-TT', 'SP012', 'A-03'), ('KHO-TT', 'SP013', 'B-01'),
  ('KHO-TT', 'SP014', 'B-01'), ('KHO-TT', 'SP015', 'B-02'), ('KHO-TT', 'SP018', 'C-01'),
  ('KHO-TT', 'SP019', 'C-01'), ('KHO-TT', 'SP021', 'C-01'),
  ('KHO-HN', 'SP001', 'A-01'), ('KHO-HN', 'SP006', 'A-02'), ('KHO-HN', 'SP009', 'A-02'),
  ('KHO-HN', 'SP011', 'A-02'), ('KHO-HN', 'SP017', 'B-01'), ('KHO-HN', 'SP020', 'B-01');

UPDATE `stock_balances` b
JOIN `warehouses` w ON w.id = b.warehouse_id
JOIN `products` p ON p.id = b.product_id
JOIN sample_sku_location s ON s.warehouse_code = w.code AND s.sku = p.sku
JOIN `warehouse_locations` l ON l.warehouse_id = w.id AND l.code = s.location_code
SET b.location_id = l.id
WHERE b.location_id IS NULL;

DROP TEMPORARY TABLE sample_sku_location;

-- Đơn mẫu đã duyệt chưa hẹn ngày giao: đơn nhỏ nhất giao hôm nay, các đơn sau giao ngày mai (giờ Việt Nam)
UPDATE `sales_orders` o
JOIN (SELECT MIN(id) AS first_id FROM `sales_orders` WHERE status = 'APPROVED') f
SET o.requested_delivery_date = CASE WHEN o.id = f.first_id THEN DATE(UTC_TIMESTAMP() + INTERVAL 7 HOUR)
                                     ELSE DATE(UTC_TIMESTAMP() + INTERVAL 7 HOUR) + INTERVAL 1 DAY END
WHERE o.status = 'APPROVED' AND o.requested_delivery_date IS NULL;

-- Lô L2610A (HSD 6 tháng tới) của SP001 tại KHO-TT: chuyển 120 từ lô mặc định sang, tổng tồn của SKU giữ nguyên
INSERT IGNORE INTO `lots` (`product_id`, `lot_no`, `expiry_date`, `is_default`, `created_at`)
SELECT p.id, 'L2610A', DATE(UTC_TIMESTAMP()) + INTERVAL 6 MONTH, false, UTC_TIMESTAMP()
FROM `products` p WHERE p.sku = 'SP001';

UPDATE `stock_lot_balances` d
JOIN `warehouses` w ON w.id = d.warehouse_id AND w.code = 'KHO-TT'
JOIN `lots` dl ON dl.id = d.lot_id AND dl.is_default
JOIN `products` p ON p.id = dl.product_id AND p.sku = 'SP001'
JOIN `lots` nl ON nl.product_id = p.id AND nl.lot_no = 'L2610A'
SET d.qty_on_hand_base = d.qty_on_hand_base - 120, d.updated_at = UTC_TIMESTAMP()
WHERE d.qty_on_hand_base >= 120
  AND NOT EXISTS (SELECT 1 FROM (SELECT * FROM `stock_lot_balances`) x WHERE x.warehouse_id = w.id AND x.lot_id = nl.id);

INSERT IGNORE INTO `stock_lot_balances` (`warehouse_id`, `lot_id`, `qty_on_hand_base`, `updated_at`)
SELECT w.id, nl.id, 120, UTC_TIMESTAMP()
FROM `warehouses` w JOIN `products` p ON p.sku = 'SP001' JOIN `lots` nl ON nl.product_id = p.id AND nl.lot_no = 'L2610A'
WHERE w.code = 'KHO-TT';
