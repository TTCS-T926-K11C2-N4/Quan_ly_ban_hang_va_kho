-- =====================================================================
-- Sprint 6 soạn - xuất - giao: S6-03 Phiếu soạn hàng (tính hàng còn phải giao), S6-05 Xác nhận xuất kho & giao thiếu,
-- S6-08 Xác nhận giao hàng. Cần chạy trước khi dùng các màn của S6-03.
--  * sales_order_items.qty_closed_base: phần thiếu đã "Đóng phần thiếu" lúc xuất kho (không giao bổ sung).
--    Hàng còn nợ của dòng đơn = qty_base - qty_shipped_base - qty_closed_base.
--  * delivery_notes: cách xử lý phần thiếu (giao bổ sung sau / đóng phần thiếu), lịch giao bổ sung, ghi chú;
--    ghi chú lúc giao hàng (S6-08). Mỗi phiếu soạn chỉ lập một phiếu xuất cho mỗi đơn.
--  * delivery_note_shortages: dòng xuất thiếu kèm lý do (dòng xuất 0 không có trong delivery_note_items).
-- Chạy một lần trên CSDL đã tạo từ bản OMS_schema_mysql.sql cũ; CSDL tạo mới từ
-- OMS_schema_mysql.sql hiện tại đã có các thay đổi này nên KHÔNG cần chạy.
-- =====================================================================

ALTER TABLE `sales_order_items`
  ADD COLUMN `qty_closed_base` decimal(18,3) NOT NULL DEFAULT 0
    COMMENT 'Phần thiếu đã đóng khi xuất kho, không giao bổ sung (S6-05)' AFTER `qty_shipped_base`,
  ADD CONSTRAINT ck_sales_order_items_4 CHECK (qty_closed_base >= 0 AND qty_shipped_base + qty_closed_base <= qty_base);

ALTER TABLE `delivery_notes`
  ADD COLUMN `shortage_action` varchar(20)
    CHECK (`shortage_action` IN ('BACKORDER','CLOSE'))
    COMMENT 'Xử lý phần thiếu: BACKORDER giao bổ sung sau | CLOSE đóng phần thiếu; NULL nếu xuất đủ' AFTER `failure_reason`,
  ADD COLUMN `backorder_due_at` datetime COMMENT 'Lịch giao bổ sung (UTC)' AFTER `shortage_action`,
  ADD COLUMN `shortage_note` text COMMENT 'Ghi chú xử lý phần thiếu' AFTER `backorder_due_at`,
  ADD COLUMN `delivery_remark` text COMMENT 'Ghi chú lúc giao hàng / ghi chú bổ sung khi giao không thành công (S6-08)' AFTER `shortage_note`,
  ADD UNIQUE KEY ux_delivery_notes_pick_order (`pick_list_id`, `order_id`),
  ADD CONSTRAINT ck_delivery_notes_1 CHECK (shortage_action IS NULL OR shortage_action <> 'BACKORDER' OR backorder_due_at IS NOT NULL);

CREATE TABLE `delivery_note_shortages` (
  `id` bigint AUTO_INCREMENT,
  `delivery_note_id` bigint NOT NULL,
  CONSTRAINT fk_delivery_note_shortages_delivery_note_id FOREIGN KEY (`delivery_note_id`) REFERENCES `delivery_notes`(`id`),
  `order_item_id` bigint NOT NULL,
  CONSTRAINT fk_delivery_note_shortages_order_item_id FOREIGN KEY (`order_item_id`) REFERENCES `sales_order_items`(`id`),
  `qty_required_base` decimal(18,3) NOT NULL,
  `qty_issued_base` decimal(18,3) NOT NULL,
  `short_reason` text NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY ux_delivery_note_shortages_note_item (`delivery_note_id`, `order_item_id`),
  CONSTRAINT ck_delivery_note_shortages_1 CHECK (qty_issued_base >= 0 AND qty_issued_base < qty_required_base),
  KEY ix_delivery_note_shortages_order_item_id (`order_item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Dòng xuất thiếu của phiếu xuất kèm lý do bắt buộc (S6-05).';
