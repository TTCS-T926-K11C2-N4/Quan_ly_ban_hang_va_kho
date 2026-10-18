-- =====================================================================
-- S7-04 Đại lý còn nợ quá hạn luôn bị chặn tạo đơn mới, không còn chế độ chuyển duyệt.
-- Mã không còn đọc cấu hình credit.overdue_mode nên xoá dòng này khỏi system_settings.
-- Chạy một lần trên CSDL dựng từ schema cũ. CSDL dựng từ OMS_schema_mysql.sql hiện tại
-- không còn dòng này nên KHÔNG cần chạy.
-- =====================================================================

DELETE FROM `system_settings` WHERE `key` = 'credit.overdue_mode';
