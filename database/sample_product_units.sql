-- =====================================================================
-- S5-04: đơn vị quy đổi mẫu (thùng, lốc) theo quy cách đóng gói của sample_products.sql, để thử nhập kho
-- theo thùng mà sổ tồn vẫn ghi theo đơn vị cơ sở (vd 10 thùng x 24 chai = 240 chai).
-- Chạy SAU sample_products.sql. Chạy lại lần 2 không lỗi: đơn vị đã khai báo thì bỏ qua.
-- =====================================================================

SET NAMES utf8mb4;

INSERT IGNORE INTO `product_units` (`product_id`, `unit_id`, `factor_to_base`, `is_base`, `created_at`, `updated_at`)
SELECT p.id, u.id, c.factor, false, UTC_TIMESTAMP(), UTC_TIMESTAMP()
FROM (SELECT 'SP001' AS sku, 'THUNG' AS unit_code, 24 AS factor
      UNION ALL SELECT 'SP001', 'LOC', 6
      UNION ALL SELECT 'SP002', 'THUNG', 24
      UNION ALL SELECT 'SP003', 'THUNG', 12
      UNION ALL SELECT 'SP006', 'THUNG', 24
      UNION ALL SELECT 'SP006', 'LOC', 6
      UNION ALL SELECT 'SP007', 'THUNG', 24
      UNION ALL SELECT 'SP009', 'THUNG', 24
      UNION ALL SELECT 'SP010', 'THUNG', 24
      UNION ALL SELECT 'SP011', 'THUNG', 24
      UNION ALL SELECT 'SP012', 'THUNG', 12
      UNION ALL SELECT 'SP013', 'THUNG', 12
      UNION ALL SELECT 'SP014', 'THUNG', 48
      UNION ALL SELECT 'SP018', 'THUNG', 12
      UNION ALL SELECT 'SP021', 'THUNG', 24) c
JOIN `products` p ON p.sku = c.sku
JOIN `units` u ON u.code = c.unit_code;
