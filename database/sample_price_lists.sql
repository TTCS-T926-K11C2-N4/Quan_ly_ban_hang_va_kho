-- =====================================================================
-- Bảng giá mẫu (S2-10) để thử màn hình Quản lý bảng giá.
-- Chạy SAU database/sample_products.sql (dòng giá lấy từ 22 sản phẩm mẫu theo SKU).
-- Chạy lại lần 2 không lỗi: bảng giá, dòng giá đã có thì bỏ qua (INSERT IGNORE theo cột UNIQUE).
-- Dùng 3 nhóm khách hàng có sẵn trong OMS_schema_mysql.sql: AGENT_L1, AGENT_L2, RETAIL.
-- Giá bán = giá vốn cộng biên lợi nhuận theo nhóm, làm tròn 500đ; giá sàn = 95% giá bán.
-- =====================================================================

SET NAMES utf8mb4;

-- Nửa đầu 2026 đã hết hạn, nửa cuối 2026 đang hiệu lực; mỗi nhóm không trùng ngày
INSERT IGNORE INTO `price_lists` (`code`, `name`, `customer_group_id`, `valid_from`, `valid_to`, `status`,
                                  `created_at`, `updated_at`)
SELECT l.code, l.name, g.id, l.valid_from, l.valid_to, 'ACTIVE', UTC_TIMESTAMP(), UTC_TIMESTAMP()
FROM (SELECT 'BG001' AS code, 'Giá đại lý cấp 1 - 6 tháng cuối 2026' AS name, 'AGENT_L1' AS group_code,
             DATE '2026-07-01' AS valid_from, DATE '2026-12-31' AS valid_to
      UNION ALL SELECT 'BG002', 'Giá đại lý cấp 2 - 6 tháng cuối 2026', 'AGENT_L2', DATE '2026-07-01', DATE '2026-12-31'
      UNION ALL SELECT 'BG003', 'Giá khách lẻ - 6 tháng cuối 2026', 'RETAIL', DATE '2026-07-01', DATE '2026-12-31'
      UNION ALL SELECT 'BG004', 'Giá đại lý cấp 1 - 6 tháng đầu 2026', 'AGENT_L1', DATE '2026-01-01', DATE '2026-06-30'
      UNION ALL SELECT 'BG005', 'Giá khách lẻ - 6 tháng đầu 2026', 'RETAIL', DATE '2026-01-01', DATE '2026-06-30') l
JOIN `customer_groups` g ON g.code = l.group_code;

INSERT IGNORE INTO `price_list_items` (`price_list_id`, `product_id`, `unit_id`, `price`, `floor_price`,
                                       `created_at`, `updated_at`)
SELECT pl.id, p.id, p.base_unit_id, ROUND(p.cost_price * m.margin / 500) * 500,
       ROUND(ROUND(p.cost_price * m.margin / 500) * 500 * 0.95 / 100) * 100, UTC_TIMESTAMP(), UTC_TIMESTAMP()
FROM `price_lists` pl
JOIN (SELECT 'BG001' AS code, 1.08 AS margin UNION ALL SELECT 'BG002', 1.12 UNION ALL SELECT 'BG003', 1.20
      UNION ALL SELECT 'BG004', 1.06 UNION ALL SELECT 'BG005', 1.18) m ON m.code = pl.code
JOIN `products` p ON p.sku BETWEEN 'SP001' AND 'SP022';
