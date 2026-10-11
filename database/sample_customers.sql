-- =====================================================================
-- Đại lý mẫu để thử màn hình S3-05 Hạn mức công nợ, S3-07 Khoá/mở giao dịch và S3-09 Tạo đơn hàng
-- khi chưa có màn hình Hồ sơ đại lý (S3-03) và Điểm giao hàng (S3-04).
-- Chạy SAU database/sample_users.sql và database/sample_warehouses_regions.sql
-- (cần nhân viên kinh doanh thaihv, địa bàn TN/HN/BN/BG, kho KHO-TT/KHO-HN/KHO-BN).
-- Chạy lại lần 2 không lỗi: đại lý trùng mã bỏ qua, điểm giao chỉ thêm cho đại lý chưa có điểm giao nào.
-- =====================================================================

SET NAMES utf8mb4;

-- thaihv (Nhân viên kinh doanh) phụ trách 3 đại lý đầu; Cửa hàng Thu Hà chưa có người phụ trách
INSERT IGNORE INTO `customers` (`code`, `name`, `tax_code`, `customer_group_id`, `region_id`, `sales_rep_id`,
                                `default_warehouse_id`, `phone`, `email`, `address`, `credit_limit`, `max_debt_days`,
                                `created_at`, `updated_at`)
SELECT c.code, c.name, c.tax_code, g.id, r.id, u.id, w.id, c.phone, c.email, c.address, c.credit_limit,
       c.max_debt_days, UTC_TIMESTAMP(), UTC_TIMESTAMP()
FROM (SELECT 'DL00125' AS code, 'Đại lý Minh Anh' AS name, '0101234567' AS tax_code, 'AGENT_L1' AS group_code,
             'HN' AS region_code, 'thaihv' AS rep, 'KHO-HN' AS warehouse_code, '0988 123 456' AS phone,
             'minhanh@example.com' AS email, '25 Nguyễn Trãi, Hà Nội' AS address, 100000000 AS credit_limit,
             30 AS max_debt_days
      UNION ALL SELECT 'DL00126', 'Đại lý Hoàng Long', '4600123456', 'AGENT_L2', 'TN', 'thaihv', 'KHO-TT',
             '0912 456 789', 'hoanglong@example.com', '12 Hoàng Văn Thụ, TP Thái Nguyên', 50000000, 15
      UNION ALL SELECT 'DL00127', 'Đại lý Phúc Thịnh', '2400123456', 'AGENT_L1', 'BG', 'thaihv', 'KHO-TT',
             '0904 222 333', NULL, '88 Xương Giang, TP Bắc Giang', 80000000, 30
      UNION ALL SELECT 'DL00128', 'Cửa hàng Thu Hà', NULL, 'RETAIL', 'BN', NULL, 'KHO-BN',
             '0977 888 999', NULL, 'Đình Bảng, Từ Sơn, Bắc Ninh', 0, 0) c
JOIN `customer_groups` g ON g.code = c.group_code
JOIN `regions` r ON r.code = c.region_code
JOIN `warehouses` w ON w.code = c.warehouse_code
LEFT JOIN `users` u ON u.username = c.rep;

INSERT INTO `customer_delivery_addresses` (`customer_id`, `label`, `address`, `receiver_name`, `receiver_phone`,
                                           `route_note`, `is_default`, `created_at`, `updated_at`)
SELECT cu.id, a.label, a.address, a.receiver_name, a.receiver_phone, a.route_note, a.is_default,
       UTC_TIMESTAMP(), UTC_TIMESTAMP()
FROM (SELECT 'DL00125' AS code, 'Cửa hàng chính' AS label, '25 Nguyễn Trãi, Thanh Xuân, Hà Nội' AS address,
             'Nguyễn Minh Anh' AS receiver_name, '0988 123 456' AS receiver_phone,
             'Xe tải vào ngõ sau, giao trước 10h' AS route_note, true AS is_default
      UNION ALL SELECT 'DL00125', 'Kho Hà Đông', 'Lô 3 KCN Phú Lãm, Hà Đông, Hà Nội', 'Trần Văn Bình',
             '0966 555 111', NULL, false
      UNION ALL SELECT 'DL00126', 'Cửa hàng chính', '12 Hoàng Văn Thụ, TP Thái Nguyên', 'Lê Hoàng Long',
             '0912 456 789', NULL, true
      UNION ALL SELECT 'DL00127', 'Cửa hàng chính', '88 Xương Giang, TP Bắc Giang', 'Phạm Phúc Thịnh',
             '0904 222 333', NULL, true
      UNION ALL SELECT 'DL00128', 'Cửa hàng', 'Đình Bảng, Từ Sơn, Bắc Ninh', 'Đỗ Thu Hà', '0977 888 999', NULL,
             true) a
JOIN `customers` cu ON cu.code = a.code
WHERE NOT EXISTS (SELECT 1 FROM `customer_delivery_addresses` d WHERE d.customer_id = cu.id);
