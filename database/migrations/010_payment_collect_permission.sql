-- =====================================================================
-- S7-05 Thu tiền theo tuyến: quyền PAYMENT_COLLECT cho Nhân viên kinh doanh (ghi nhận khoản thu tại cửa hàng,
-- trạng thái Chờ nộp cho tới khi kế toán xác nhận đã nhận tiền).
-- Chạy một lần trên CSDL đã nạp seed_permissions.sql cũ. CSDL nạp lại bằng seed_permissions.sql hiện tại
-- đã có quyền này nên KHÔNG cần chạy.
-- =====================================================================

INSERT INTO `permissions` (`code`, `module`, `action`, `description`)
VALUES ('PAYMENT_COLLECT', 'Hoá đơn & công nợ', 'CREATE', 'Thu tiền theo tuyến tại cửa hàng, chờ nộp về công ty')
ON DUPLICATE KEY UPDATE `module` = VALUES(`module`), `action` = VALUES(`action`), `description` = VALUES(`description`);

INSERT IGNORE INTO `role_permissions` (`role_id`, `permission_id`, `data_scope`)
SELECT r.`id`, p.`id`, 'ASSIGNED'
FROM `roles` r
JOIN `permissions` p ON p.`code` = 'PAYMENT_COLLECT'
WHERE r.`code` = 'SALES_REP';
