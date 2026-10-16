-- =====================================================================
-- S5-03 Kho và vị trí lưu trong kho:
--  * warehouses.description: mô tả ngắn hiện dưới tên kho ở danh sách kho (vd "Kho chính").
--  * warehouse_locations.parent_id: kệ thuộc khu nào, để hiện cây vị trí Khu > Kệ.
-- Chạy một lần trên CSDL đã tạo từ bản OMS_schema_mysql.sql cũ; CSDL tạo mới từ
-- OMS_schema_mysql.sql hiện tại đã có các cột này nên KHÔNG cần chạy.
-- =====================================================================

ALTER TABLE `warehouses`
  ADD COLUMN `description` varchar(200) NULL
    COMMENT 'Mô tả ngắn hiện dưới tên kho, vd Kho chính, Khu cách ly hàng lỗi (S5-03)' AFTER `name`;

ALTER TABLE `warehouse_locations`
  ADD COLUMN `parent_id` bigint NULL
    COMMENT 'Khu chứa kệ này (cây vị trí Khu > Kệ, S5-03); NULL với khu và kệ đứng riêng' AFTER `warehouse_id`,
  ADD CONSTRAINT fk_warehouse_locations_parent_id FOREIGN KEY (`parent_id`) REFERENCES `warehouse_locations`(`id`),
  ADD KEY ix_warehouse_locations_parent_id (`parent_id`);
