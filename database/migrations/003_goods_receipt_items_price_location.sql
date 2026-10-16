-- =====================================================================
-- S5-04 Phiếu nhập kho: mỗi dòng hàng có đơn giá nhập (thành tiền = số lượng x đơn giá) và vị trí cất hàng
-- trong kho nhập, theo màn Tạo phiếu nhập kho.
-- Chạy một lần trên CSDL đã tạo từ bản OMS_schema_mysql.sql cũ; CSDL tạo mới từ
-- OMS_schema_mysql.sql hiện tại đã có các cột này nên KHÔNG cần chạy.
-- =====================================================================

ALTER TABLE `goods_receipt_items`
  ADD COLUMN `unit_price` decimal(18,2) NOT NULL DEFAULT 0
    COMMENT 'Đơn giá nhập theo đơn vị nhập (unit_id); thành tiền = qty * unit_price (S5-04)' AFTER `qty_base`,
  ADD COLUMN `location_id` bigint NULL COMMENT 'Vị trí cất hàng trong kho nhập (S5-04)' AFTER `unit_price`,
  ADD CONSTRAINT fk_goods_receipt_items_location_id FOREIGN KEY (`location_id`) REFERENCES `warehouse_locations`(`id`),
  ADD CONSTRAINT ck_goods_receipt_items_2 CHECK (unit_price >= 0),
  ADD KEY ix_goods_receipt_items_location_id (`location_id`);
