-- =====================================================================
-- S5-03: mô tả, người phụ trách và vị trí lưu (khu, kệ) mẫu cho các kho trong sample_warehouses_regions.sql,
-- để dựng và thử màn Danh sách kho & vị trí. Chạy SAU sample_warehouses_regions.sql và sample_users.sql
-- (CSDL cũ chạy trước database/migrations/002_warehouse_description_location_parent.sql).
-- Chạy lại lần 2 không lỗi: mô tả, người phụ trách chỉ điền khi còn trống; vị trí đã có thì bỏ qua (INSERT IGNORE).
-- =====================================================================

SET NAMES utf8mb4;

UPDATE `warehouses` w
JOIN (SELECT 'KHO-TT' AS code, 'Kho chính' AS description, 'tuyetnt' AS manager
      UNION ALL SELECT 'KHO-HN', 'Kho khu vực', 'danlt'
      UNION ALL SELECT 'KHO-BN', 'Kho khu vực', NULL
      UNION ALL SELECT 'KHO-LOI', 'Khu cách ly hàng lỗi', 'tuyetnt') d ON d.code = w.code
LEFT JOIN `users` u ON u.username = d.manager
SET w.description = COALESCE(w.description, d.description),
    w.manager_user_id = COALESCE(w.manager_user_id, u.id),
    w.updated_at = UTC_TIMESTAMP();

-- Khu
INSERT IGNORE INTO `warehouse_locations` (`warehouse_id`, `parent_id`, `location_type`, `code`, `name`, `pick_sequence`,
                                          `is_active`, `created_at`, `updated_at`)
SELECT w.id, NULL, 'ZONE', z.code, z.name, z.seq, true, UTC_TIMESTAMP(), UTC_TIMESTAMP()
FROM (SELECT 'KHO-TT' AS warehouse, 'A' AS code, 'Khu đồ uống' AS name, 10 AS seq
      UNION ALL SELECT 'KHO-TT', 'B', 'Khu bánh kẹo', 20
      UNION ALL SELECT 'KHO-TT', 'C', 'Khu hàng chờ xuất', 30
      UNION ALL SELECT 'KHO-HN', 'A', 'Khu đồ uống', 10
      UNION ALL SELECT 'KHO-HN', 'B', 'Khu bánh kẹo', 20
      UNION ALL SELECT 'KHO-LOI', 'Q', 'Khu cách ly', 10) z
JOIN `warehouses` w ON w.code = z.warehouse;

-- Kệ trong khu (mã kệ = mã khu + số kệ); thứ tự đi soạn hàng tiếp theo thứ tự của khu
INSERT IGNORE INTO `warehouse_locations` (`warehouse_id`, `parent_id`, `location_type`, `code`, `name`, `pick_sequence`,
                                          `is_active`, `created_at`, `updated_at`)
SELECT w.id, zone.id, 'RACK', r.code, r.name, r.seq, true, UTC_TIMESTAMP(), UTC_TIMESTAMP()
FROM (SELECT 'KHO-TT' AS warehouse, 'A' AS zone_code, 'A-01' AS code, 'Kệ bia lon' AS name, 11 AS seq
      UNION ALL SELECT 'KHO-TT', 'A', 'A-02', 'Kệ bia chai', 12
      UNION ALL SELECT 'KHO-TT', 'A', 'A-03', 'Kệ nước ngọt, nước suối', 13
      UNION ALL SELECT 'KHO-TT', 'B', 'B-01', 'Kệ bánh', 21
      UNION ALL SELECT 'KHO-TT', 'B', 'B-02', 'Kệ kẹo', 22
      UNION ALL SELECT 'KHO-TT', 'C', 'C-01', 'Bãi tập kết', 31
      UNION ALL SELECT 'KHO-HN', 'A', 'A-01', 'Kệ bia', 11
      UNION ALL SELECT 'KHO-HN', 'A', 'A-02', 'Kệ nước giải khát', 12
      UNION ALL SELECT 'KHO-HN', 'B', 'B-01', 'Kệ bánh kẹo', 21
      UNION ALL SELECT 'KHO-LOI', 'Q', 'Q-01', 'Kệ hàng chờ xử lý', 11) r
JOIN `warehouses` w ON w.code = r.warehouse
JOIN (SELECT id, warehouse_id, code FROM `warehouse_locations` WHERE `location_type` = 'ZONE') zone
  ON zone.warehouse_id = w.id AND zone.code = r.zone_code;
