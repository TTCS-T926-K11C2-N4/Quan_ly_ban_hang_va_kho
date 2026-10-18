-- =====================================================================
-- S5-06 Giữ chỗ cho đơn đã duyệt hết hạn vào cuối ngày giao mong muốn (tối thiểu inventory.reservation_ttl_hours giờ
-- kể từ lúc duyệt); hết hạn thì nhả hàng và báo nhân viên kinh doanh phụ trách đại lý. Cập nhật lại mô tả cấu hình.
-- Chạy một lần trên CSDL dựng từ schema cũ. CSDL dựng từ OMS_schema_mysql.sql hiện tại đã có mô tả này nên KHÔNG
-- cần chạy. Giá trị cấu hình giữ nguyên.
-- =====================================================================

UPDATE `system_settings`
SET `description` = 'Giữ chỗ cho đơn đã duyệt hết hạn vào cuối ngày giao mong muốn nhưng không sớm hơn số giờ này kể từ lúc duyệt; đơn không có ngày giao giữ đúng số giờ này'
WHERE `key` = 'inventory.reservation_ttl_hours';
