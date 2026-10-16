-- =====================================================================
-- S6-01 Lô và hạn sử dụng: lô đã quá hạn không được soạn / xuất, trừ khi Quản lý kho xác nhận ghi đè kèm lý do.
--  * lots.expired_issue_*: người xác nhận, thời điểm và lý do cho phép xuất lô quá hạn. NULL = chưa cho phép.
-- Chạy một lần trên CSDL đã tạo từ bản OMS_schema_mysql.sql cũ; CSDL tạo mới từ
-- OMS_schema_mysql.sql hiện tại đã có các cột này nên KHÔNG cần chạy.
-- =====================================================================

ALTER TABLE `lots`
  ADD COLUMN `expired_issue_approved_by` bigint COMMENT 'Quản lý kho cho phép xuất lô đã quá hạn (S6-01)' AFTER `is_default`,
  ADD COLUMN `expired_issue_approved_at` datetime COMMENT 'Thời điểm cho phép (UTC)' AFTER `expired_issue_approved_by`,
  ADD COLUMN `expired_issue_reason` text COMMENT 'Lý do bắt buộc khi cho phép' AFTER `expired_issue_approved_at`,
  ADD CONSTRAINT fk_lots_expired_issue_approved_by FOREIGN KEY (`expired_issue_approved_by`) REFERENCES `users`(`id`);
