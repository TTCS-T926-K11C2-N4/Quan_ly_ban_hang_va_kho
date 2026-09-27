-- =====================================================================
-- Tài khoản mẫu để thử màn hình S1-08 Danh sách tài khoản người dùng.
-- Chạy SAU khi đã import OMS_schema_mysql.sql (cần dữ liệu bảng roles).
-- Mật khẩu của mọi tài khoản mẫu: Test@1234 (đã băm bcrypt, cost 10).
-- Chạy lại lần 2 sẽ lỗi trùng username/email: xoá các tài khoản này trước nếu cần.
-- =====================================================================

SET NAMES utf8mb4;

-- created_at giảm dần để khi sắp xếp mới nhất lên đầu, thứ tự khớp Figma.
-- Mọi DATETIME lưu theo UTC (xem ghi chú đầu OMS_schema_mysql.sql).
INSERT INTO `users`
  (`username`, `email`, `phone`, `full_name`, `password_hash`, `status`,
   `must_change_password`, `locked_until`, `lock_reason`, `created_at`, `updated_at`)
VALUES
  ('tuyetnt', 'tuyetnt@example.com', '0912345678', 'Nguyễn Thị Tuyết',
   '$2a$10$GkdI7329OwW2nlsAO2onsuOCxJWD2bDM/zgzNIrxgB3WPVRT3ThXa', 'ACTIVE', false, NULL, NULL,
   UTC_TIMESTAMP() - INTERVAL 1 MINUTE, UTC_TIMESTAMP()),
  ('thaihv', 'thaihv@example.com', '0987654321', 'Hoàng Văn Thái',
   '$2a$10$GkdI7329OwW2nlsAO2onsuOCxJWD2bDM/zgzNIrxgB3WPVRT3ThXa', 'ACTIVE', false, NULL, NULL,
   UTC_TIMESTAMP() - INTERVAL 2 MINUTE, UTC_TIMESTAMP()),
  ('vinhtt', 'vinhtt@example.com', '0905123456', 'Trương Thế Vinh',
   '$2a$10$GkdI7329OwW2nlsAO2onsuOCxJWD2bDM/zgzNIrxgB3WPVRT3ThXa', 'ACTIVE', false, NULL, NULL,
   UTC_TIMESTAMP() - INTERVAL 3 MINUTE, UTC_TIMESTAMP()),
  -- Tạm khóa: khóa tự động 15 phút sau 5 lần sai mật khẩu, hết 15 phút sẽ hiện lại "Hoạt động"
  ('danlt', 'danlt@example.com', '0934567890', 'Lã Thế Đan',
   '$2a$10$GkdI7329OwW2nlsAO2onsuOCxJWD2bDM/zgzNIrxgB3WPVRT3ThXa', 'ACTIVE', false,
   UTC_TIMESTAMP() + INTERVAL 15 MINUTE, NULL,
   UTC_TIMESTAMP() - INTERVAL 4 MINUTE, UTC_TIMESTAMP()),
  ('anhbvn', 'anhbvn@example.com', '0978123456', 'Bùi Vũ Nguyên Anh',
   '$2a$10$GkdI7329OwW2nlsAO2onsuOCxJWD2bDM/zgzNIrxgB3WPVRT3ThXa', 'ACTIVE', false, NULL, NULL,
   UTC_TIMESTAMP() - INTERVAL 5 MINUTE, UTC_TIMESTAMP()),
  ('huyenbt', 'huyenbt@example.com', '0967890123', 'Bùi Thanh Huyền',
   '$2a$10$GkdI7329OwW2nlsAO2onsuOCxJWD2bDM/zgzNIrxgB3WPVRT3ThXa', 'ACTIVE', false, NULL, NULL,
   UTC_TIMESTAMP() - INTERVAL 6 MINUTE, UTC_TIMESTAMP()),
  ('huanbx', 'huanbx@example.com', '0945678901', 'Bùi Xuân Huân',
   '$2a$10$GkdI7329OwW2nlsAO2onsuOCxJWD2bDM/zgzNIrxgB3WPVRT3ThXa', 'LOCKED', false, NULL,
   'Dữ liệu mẫu: khóa để thử giao diện',
   UTC_TIMESTAMP() - INTERVAL 7 MINUTE, UTC_TIMESTAMP()),
  ('khoadm', 'khoadm@example.com', '0911222333', 'Đỗ Minh Khoa',
   '$2a$10$GkdI7329OwW2nlsAO2onsuOCxJWD2bDM/zgzNIrxgB3WPVRT3ThXa', 'PENDING', true, NULL, NULL,
   UTC_TIMESTAMP() - INTERVAL 8 MINUTE, UTC_TIMESTAMP());

INSERT INTO `user_roles` (`user_id`, `role_id`)
SELECT u.`id`, r.`id`
FROM `users` u
JOIN `roles` r ON (u.`username`, r.`code`) IN (
  ('tuyetnt', 'WH_MANAGER'),
  ('thaihv',  'SALES_REP'),
  ('vinhtt',  'ACCOUNTANT'),
  ('danlt',   'WAREHOUSE'),
  ('anhbvn',  'CUSTOMER'),
  ('huyenbt', 'SALES_MANAGER'),
  ('huanbx',  'ADMIN'),
  ('khoadm',  'WAREHOUSE')
);
