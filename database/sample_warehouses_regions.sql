-- =====================================================================
-- Kho và địa bàn mẫu để chọn trong màn hình S1-09A Tạo tài khoản người dùng.
-- Chạy SAU khi đã import OMS_schema_mysql.sql. Chạy lại lần 2 sẽ lỗi trùng code.
-- Ghi created_at/updated_at bằng UTC_TIMESTAMP() vì mọi DATETIME lưu theo UTC.
-- =====================================================================

SET NAMES utf8mb4;

INSERT INTO `warehouses` (`code`, `name`, `address`, `warehouse_type`, `status`, `created_at`, `updated_at`)
VALUES
  ('KHO-TT',  'Kho trung tâm',      'TP Thái Nguyên, Thái Nguyên', 'SELLABLE', 'ACTIVE', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  ('KHO-HN',  'Kho Hà Nội',         'Long Biên, Hà Nội',           'SELLABLE', 'ACTIVE', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  ('KHO-BN',  'Kho Bắc Ninh',       'Từ Sơn, Bắc Ninh',            'SELLABLE', 'ACTIVE', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  ('KHO-LOI', 'Kho hàng lỗi',       'TP Thái Nguyên, Thái Nguyên', 'DEFECT',   'ACTIVE', UTC_TIMESTAMP(), UTC_TIMESTAMP());

INSERT INTO `regions` (`code`, `name`, `created_at`, `updated_at`)
VALUES
  ('TN', 'Thái Nguyên', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  ('HN', 'Hà Nội',      UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  ('BN', 'Bắc Ninh',    UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  ('BG', 'Bắc Giang',   UTC_TIMESTAMP(), UTC_TIMESTAMP());
