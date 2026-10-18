-- =====================================================================
-- S8-04 Chỉ tiêu doanh số: quyền SALES_TARGET_MANAGE cho Quản lý kinh doanh (đặt và sửa chỉ tiêu theo tháng).
-- Nhân viên kinh doanh xem tiến độ của chính mình qua quyền xem báo cáo REPORT_VIEW.
-- Chạy một lần trên CSDL đã nạp seed_permissions.sql cũ. CSDL nạp lại bằng seed_permissions.sql hiện tại
-- đã có quyền này nên KHÔNG cần chạy.
-- =====================================================================

INSERT INTO `permissions` (`code`, `module`, `action`, `description`)
VALUES ('SALES_TARGET_MANAGE', 'Báo cáo & dashboard', 'MANAGE', 'Đặt và sửa chỉ tiêu doanh số theo tháng cho nhân viên và khu vực')
ON DUPLICATE KEY UPDATE `module` = VALUES(`module`), `action` = VALUES(`action`), `description` = VALUES(`description`);

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`, `data_scope`)
SELECT r.`id`, p.`id`, 'ALL'
FROM `roles` r
JOIN `permissions` p ON p.`code` = 'SALES_TARGET_MANAGE'
WHERE r.`code` = 'SALES_MANAGER';
