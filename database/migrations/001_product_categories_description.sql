-- =====================================================================
-- S2-06 Quản lý nhóm hàng: thêm mô tả cho nhóm hàng (màn hình có cột "Mô tả").
-- Chạy một lần trên CSDL đã tạo từ bản OMS_schema_mysql.sql cũ; CSDL tạo mới từ
-- OMS_schema_mysql.sql hiện tại đã có cột này nên KHÔNG cần chạy.
-- =====================================================================

ALTER TABLE `product_categories`
  ADD COLUMN `description` varchar(500) NULL AFTER `name`;
