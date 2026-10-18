-- =====================================================================
-- S5-06: đơn đã duyệt đang giữ chỗ tồn mẫu, để dựng và thử màn Chi tiết giữ chỗ tồn khi chưa có màn duyệt đơn
-- (S4-05). Ba đơn đang giữ chỗ (duyệt 1-20 giờ trước) và một đơn đã hết hạn giữ chỗ (duyệt 3 ngày trước).
-- Chạy SAU sample_customers.sql và sample_products.sql (cần đại lý DL00125-DL00127, kho KHO-TT, KHO-HN).
-- Phần tồn đầu kỳ bên dưới giống hệt các dòng tương ứng của sample_stock.sql (S5-05): chạy file nào trước cũng được,
-- dòng tồn đã có thì bỏ qua. Chạy lại lần 2 không lỗi và không giữ chỗ trùng.
-- =====================================================================

SET NAMES utf8mb4;

-- ----- Tồn đầu kỳ của các SKU được giữ chỗ (cùng số liệu với sample_stock.sql) -----
DROP TEMPORARY TABLE IF EXISTS sample_opening_stock;
CREATE TEMPORARY TABLE sample_opening_stock (
  warehouse_code varchar(30), sku varchar(50), qty decimal(18,3), min_stock decimal(18,3)
);
INSERT INTO sample_opening_stock VALUES
  ('KHO-TT', 'SP001', 480, 120), ('KHO-TT', 'SP002', 240, 120), ('KHO-TT', 'SP006', 360, 100),
  ('KHO-TT', 'SP011', 600, 200), ('KHO-TT', 'SP012', 144, 60),  ('KHO-TT', 'SP021', 85, 0),
  ('KHO-HN', 'SP001', 240, 96),  ('KHO-HN', 'SP011', 300, 120);

INSERT IGNORE INTO `lots` (`product_id`, `lot_no`, `is_default`, `created_at`)
SELECT DISTINCT p.id, 'MAC-DINH', true, UTC_TIMESTAMP()
FROM sample_opening_stock s JOIN `products` p ON p.sku = s.sku
WHERE NOT EXISTS (SELECT 1 FROM `lots` l WHERE l.product_id = p.id AND l.is_default);

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

-- ----- Đơn đã duyệt (mã DHMAU-xxxx để không trùng mã đơn tạo trên giao diện) -----
INSERT IGNORE INTO `sales_orders` (`order_no`, `customer_id`, `delivery_address_id`, `warehouse_id`, `sales_rep_id`,
                                   `source`, `status`, `needs_approval`, `submitted_at`, `approved_at`, `note`,
                                   `created_at`, `created_by`, `updated_at`, `updated_by`)
SELECT o.order_no, c.id,
       (SELECT a.id FROM `customer_delivery_addresses` a WHERE a.customer_id = c.id ORDER BY a.is_default DESC, a.id LIMIT 1),
       c.default_warehouse_id, c.sales_rep_id, 'SALES_REP', 'APPROVED', false,
       UTC_TIMESTAMP() - INTERVAL (o.approved_hours_ago + 2) HOUR, UTC_TIMESTAMP() - INTERVAL o.approved_hours_ago HOUR,
       'Đơn mẫu giữ chỗ tồn (S5-06)', UTC_TIMESTAMP() - INTERVAL (o.approved_hours_ago + 2) HOUR, c.sales_rep_id,
       UTC_TIMESTAMP() - INTERVAL o.approved_hours_ago HOUR, c.sales_rep_id
FROM (SELECT 'DHMAU-0001' AS order_no, 'DL00126' AS customer_code, 3 AS approved_hours_ago
      UNION ALL SELECT 'DHMAU-0002', 'DL00127', 20
      UNION ALL SELECT 'DHMAU-0003', 'DL00125', 1
      UNION ALL SELECT 'DHMAU-0004', 'DL00126', 72) o
JOIN `customers` c ON c.code = o.customer_code
WHERE c.default_warehouse_id IS NOT NULL
  AND EXISTS (SELECT 1 FROM `customer_delivery_addresses` a WHERE a.customer_id = c.id);

-- Dòng hàng theo đơn vị cơ sở (chỉ thêm cho đơn mẫu chưa có dòng nào)
INSERT INTO `sales_order_items` (`order_id`, `line_no`, `product_id`, `unit_id`, `unit_factor`, `qty`, `qty_base`,
                                  `unit_price`, `floor_price`, `line_total`, `created_at`)
SELECT so.id, i.line_no, p.id, p.base_unit_id, 1, i.qty, i.qty, i.price, 0, i.qty * i.price, so.created_at
FROM (SELECT 'DHMAU-0001' AS order_no, 1 AS line_no, 'SP011' AS sku, 24 AS qty, 4500 AS price
      UNION ALL SELECT 'DHMAU-0001', 2, 'SP001', 48, 11500
      UNION ALL SELECT 'DHMAU-0001', 3, 'SP021', 12, 15000
      UNION ALL SELECT 'DHMAU-0002', 1, 'SP006', 120, 9000
      UNION ALL SELECT 'DHMAU-0002', 2, 'SP012', 24, 9500
      UNION ALL SELECT 'DHMAU-0003', 1, 'SP001', 48, 11500
      UNION ALL SELECT 'DHMAU-0003', 2, 'SP011', 16, 4500
      UNION ALL SELECT 'DHMAU-0004', 1, 'SP002', 24, 14500) i
JOIN `sales_orders` so ON so.order_no = i.order_no
JOIN `products` p ON p.sku = i.sku
WHERE NOT EXISTS (SELECT 1 FROM `sales_order_items` x WHERE x.order_id = so.id);

UPDATE `sales_orders` o
JOIN (SELECT order_id, SUM(line_total) AS total FROM `sales_order_items` GROUP BY order_id) s ON s.order_id = o.id
SET o.subtotal_amount = s.total, o.total_amount = s.total
WHERE o.order_no LIKE 'DHMAU-%';

-- Vòng đời đơn: gửi duyệt rồi được duyệt
INSERT INTO `order_status_history` (`order_id`, `from_status`, `to_status`, `note`, `created_at`, `created_by`)
SELECT so.id, h.from_status, h.to_status, h.note,
       CASE WHEN h.to_status = 'APPROVED' THEN so.approved_at ELSE so.submitted_at END, so.sales_rep_id
FROM `sales_orders` so
JOIN (SELECT NULL AS from_status, 'PENDING_APPROVAL' AS to_status, 'Tạo đơn và gửi duyệt' AS note
      UNION ALL SELECT 'PENDING_APPROVAL', 'APPROVED', 'Duyệt đơn (dữ liệu mẫu)') h
WHERE so.order_no LIKE 'DHMAU-%'
  AND NOT EXISTS (SELECT 1 FROM `order_status_history` x WHERE x.order_id = so.id);

-- Giữ chỗ lúc duyệt, hạn 48 giờ (inventory.reservation_ttl_hours); DHMAU-0004 đã quá hạn nên đã nhả
INSERT INTO `stock_reservations` (`order_item_id`, `warehouse_id`, `product_id`, `qty_base`, `status`, `reserved_at`,
                                  `expires_at`, `released_at`, `release_reason`, `created_at`, `created_by`)
SELECT i.id, so.warehouse_id, i.product_id, i.qty_base,
       CASE WHEN so.order_no = 'DHMAU-0004' THEN 'RELEASED' ELSE 'ACTIVE' END,
       so.approved_at, so.approved_at + INTERVAL 48 HOUR,
       CASE WHEN so.order_no = 'DHMAU-0004' THEN so.approved_at + INTERVAL 48 HOUR END,
       CASE WHEN so.order_no = 'DHMAU-0004' THEN 'EXPIRED' END,
       so.approved_at, so.sales_rep_id
FROM `sales_order_items` i
JOIN `sales_orders` so ON so.id = i.order_id
WHERE so.order_no LIKE 'DHMAU-%'
  AND NOT EXISTS (SELECT 1 FROM `stock_reservations` r WHERE r.order_item_id = i.id);

-- Sổ tồn: lượng đang giữ chỗ = tổng giữ chỗ ACTIVE theo (kho, SKU)
UPDATE `stock_balances` b
SET b.qty_reserved_base = (SELECT COALESCE(SUM(r.qty_base), 0) FROM `stock_reservations` r
                           WHERE r.warehouse_id = b.warehouse_id AND r.product_id = b.product_id AND r.status = 'ACTIVE');
