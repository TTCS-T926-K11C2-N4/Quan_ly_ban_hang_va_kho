-- =====================================================================
-- S5-08 Kiểm kê kho: quyền riêng STOCKTAKE_FINALIZE để chốt phiếu kiểm kê (điều chỉnh tồn về số đếm).
-- Chỉ Quản lý kho (WH_MANAGER) có; Admin cũng không có (như CREDIT_MANAGE), nhân viên kho chỉ tạo phiếu và nhập số đếm.
-- Chạy một lần trên CSDL đã nạp seed_permissions.sql cũ. CSDL nạp lại bằng seed_permissions.sql hiện tại
-- đã có quyền này nên KHÔNG cần chạy.
-- =====================================================================

INSERT INTO `permissions` (`code`, `module`, `action`, `description`)
VALUES ('STOCKTAKE_FINALIZE', 'Tồn kho & nhập kho', 'APPROVE', 'Chốt phiếu kiểm kê, điều chỉnh tồn về số đếm')
ON DUPLICATE KEY UPDATE `module` = VALUES(`module`), `action` = VALUES(`action`), `description` = VALUES(`description`);

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`, `data_scope`)
SELECT r.`id`, p.`id`, 'ALL'
FROM `roles` r
JOIN `permissions` p ON p.`code` = 'STOCKTAKE_FINALIZE'
WHERE r.`code` = 'WH_MANAGER';
