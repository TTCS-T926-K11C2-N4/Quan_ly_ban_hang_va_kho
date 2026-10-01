-- =====================================================================
-- Quyền của 7 vai trò nghiệp vụ (S1-05), lấy từ "MA TRẬN PHÂN QUYỀN THEO MODULE"
-- trong sheet "2. User Roles" của file HỆ THỐNG BÁN HÀNG & KHO.xlsx.
-- Chạy SAU OMS_schema_mysql.sql (cần dữ liệu bảng roles). Chạy lại nhiều lần được:
-- permissions được cập nhật, role_permissions được xoá và nạp lại toàn bộ.
--
-- Quy ước quy đổi từ ma trận:
--   R  -> *_VIEW              W / F -> *_VIEW + *_MANAGE
--   có dấu * -> data_scope OWN (Đại lý) hoặc ASSIGNED (Nhân viên kinh doanh), còn lại ALL
--   Khác biệt W và F (ghi được trường nào, thao tác nào) kiểm ở từng chức năng khi làm module đó.
-- Giá vốn và biên lợi nhuận: quyền riêng COST_PRICE_VIEW, chỉ Quản lý kinh doanh có (kể cả Admin cũng không).
-- Nhật ký thao tác: quyền riêng AUDIT_LOG_VIEW, chỉ Admin có (S2-04), dù Quản lý kinh doanh có USER_VIEW.
-- ADMIN có mọi quyền còn lại (xem câu lệnh cuối file).
--
-- src/test/java/com/oms/security/AccessRulesTest.java đọc trực tiếp file này:
-- giữ nguyên dạng mỗi quyền một dòng ('CODE', ...) và ROW('ROLE', 'CODE', 'SCOPE').
-- =====================================================================

SET NAMES utf8mb4;

START TRANSACTION;

INSERT INTO `permissions` (`code`, `module`, `action`, `description`) VALUES
  ('PRODUCT_VIEW', 'Sản phẩm & bảng giá', 'VIEW', 'Xem sản phẩm và bảng giá'),
  ('PRODUCT_MANAGE', 'Sản phẩm & bảng giá', 'MANAGE', 'Thêm, sửa sản phẩm và bảng giá'),
  ('COST_PRICE_VIEW', 'Sản phẩm & bảng giá', 'VIEW', 'Xem giá vốn và biên lợi nhuận'),
  ('CUSTOMER_VIEW', 'Đại lý & hạn mức công nợ', 'VIEW', 'Xem đại lý và hạn mức công nợ'),
  ('CUSTOMER_MANAGE', 'Đại lý & hạn mức công nợ', 'MANAGE', 'Thêm, sửa đại lý và hạn mức công nợ'),
  ('ORDER_VIEW', 'Đơn hàng', 'VIEW', 'Xem đơn hàng'),
  ('ORDER_MANAGE', 'Đơn hàng', 'MANAGE', 'Tạo, sửa, huỷ đơn hàng'),
  ('ORDER_APPROVAL_VIEW', 'Duyệt đơn & giá đặc biệt', 'VIEW', 'Xem đơn chờ duyệt'),
  ('ORDER_APPROVE', 'Duyệt đơn & giá đặc biệt', 'APPROVE', 'Duyệt đơn vượt hạn mức, giá dưới giá sàn'),
  ('INVENTORY_VIEW', 'Tồn kho & nhập kho', 'VIEW', 'Xem tồn kho và phiếu nhập'),
  ('INVENTORY_MANAGE', 'Tồn kho & nhập kho', 'MANAGE', 'Nhập kho, chuyển kho, kiểm kê'),
  ('DELIVERY_VIEW', 'Phiếu xuất & giao hàng', 'VIEW', 'Xem phiếu xuất và giao hàng'),
  ('DELIVERY_MANAGE', 'Phiếu xuất & giao hàng', 'MANAGE', 'Soạn, xuất và giao hàng'),
  ('INVOICE_VIEW', 'Hoá đơn & công nợ', 'VIEW', 'Xem hoá đơn và công nợ'),
  ('INVOICE_MANAGE', 'Hoá đơn & công nợ', 'MANAGE', 'Phát hành hoá đơn, ghi nhận thanh toán'),
  ('RETURN_VIEW', 'Trả hàng & điều chỉnh', 'VIEW', 'Xem phiếu trả hàng và điều chỉnh'),
  ('RETURN_MANAGE', 'Trả hàng & điều chỉnh', 'MANAGE', 'Lập phiếu trả hàng và điều chỉnh'),
  ('REPORT_VIEW', 'Báo cáo & dashboard', 'VIEW', 'Xem báo cáo và dashboard'),
  ('USER_VIEW', 'Người dùng & nhật ký', 'VIEW', 'Xem tài khoản người dùng và nhật ký'),
  ('USER_MANAGE', 'Người dùng & nhật ký', 'MANAGE', 'Tạo, sửa, khoá tài khoản người dùng'),
  ('AUDIT_LOG_VIEW', 'Người dùng & nhật ký', 'VIEW', 'Xem nhật ký thao tác trên tồn kho, giá, công nợ, hoá đơn')
AS new_row
ON DUPLICATE KEY UPDATE `module` = new_row.`module`, `action` = new_row.`action`,
  `description` = new_row.`description`;

DELETE FROM `role_permissions`;

INSERT INTO `role_permissions` (`role_id`, `permission_id`, `data_scope`)
SELECT r.`id`, p.`id`, m.data_scope
FROM (VALUES
  -- Đại lý (Customer)
  ROW('CUSTOMER', 'PRODUCT_VIEW', 'OWN'),
  ROW('CUSTOMER', 'CUSTOMER_VIEW', 'OWN'),
  ROW('CUSTOMER', 'ORDER_VIEW', 'OWN'),
  ROW('CUSTOMER', 'ORDER_MANAGE', 'OWN'),
  ROW('CUSTOMER', 'DELIVERY_VIEW', 'OWN'),
  ROW('CUSTOMER', 'INVOICE_VIEW', 'OWN'),
  ROW('CUSTOMER', 'RETURN_VIEW', 'OWN'),
  ROW('CUSTOMER', 'RETURN_MANAGE', 'OWN'),
  -- Nhân viên kinh doanh (Sales Rep)
  ROW('SALES_REP', 'PRODUCT_VIEW', 'ALL'),
  ROW('SALES_REP', 'CUSTOMER_VIEW', 'ASSIGNED'),
  ROW('SALES_REP', 'CUSTOMER_MANAGE', 'ASSIGNED'),
  ROW('SALES_REP', 'ORDER_VIEW', 'ASSIGNED'),
  ROW('SALES_REP', 'ORDER_MANAGE', 'ASSIGNED'),
  ROW('SALES_REP', 'INVENTORY_VIEW', 'ALL'),
  ROW('SALES_REP', 'DELIVERY_VIEW', 'ASSIGNED'),
  ROW('SALES_REP', 'INVOICE_VIEW', 'ASSIGNED'),
  ROW('SALES_REP', 'RETURN_VIEW', 'ASSIGNED'),
  ROW('SALES_REP', 'RETURN_MANAGE', 'ASSIGNED'),
  ROW('SALES_REP', 'REPORT_VIEW', 'ASSIGNED'),
  -- Nhân viên kho (Warehouse)
  ROW('WAREHOUSE', 'PRODUCT_VIEW', 'ALL'),
  ROW('WAREHOUSE', 'ORDER_VIEW', 'ALL'),
  ROW('WAREHOUSE', 'INVENTORY_VIEW', 'ALL'),
  ROW('WAREHOUSE', 'INVENTORY_MANAGE', 'ALL'),
  ROW('WAREHOUSE', 'DELIVERY_VIEW', 'ALL'),
  ROW('WAREHOUSE', 'DELIVERY_MANAGE', 'ALL'),
  ROW('WAREHOUSE', 'RETURN_VIEW', 'ALL'),
  ROW('WAREHOUSE', 'RETURN_MANAGE', 'ALL'),
  -- Quản lý kho (WH Manager)
  ROW('WH_MANAGER', 'PRODUCT_VIEW', 'ALL'),
  ROW('WH_MANAGER', 'ORDER_VIEW', 'ALL'),
  ROW('WH_MANAGER', 'INVENTORY_VIEW', 'ALL'),
  ROW('WH_MANAGER', 'INVENTORY_MANAGE', 'ALL'),
  ROW('WH_MANAGER', 'DELIVERY_VIEW', 'ALL'),
  ROW('WH_MANAGER', 'DELIVERY_MANAGE', 'ALL'),
  ROW('WH_MANAGER', 'RETURN_VIEW', 'ALL'),
  ROW('WH_MANAGER', 'RETURN_MANAGE', 'ALL'),
  ROW('WH_MANAGER', 'REPORT_VIEW', 'ALL'),
  -- Kế toán công nợ (Accountant)
  ROW('ACCOUNTANT', 'PRODUCT_VIEW', 'ALL'),
  ROW('ACCOUNTANT', 'CUSTOMER_VIEW', 'ALL'),
  ROW('ACCOUNTANT', 'CUSTOMER_MANAGE', 'ALL'),
  ROW('ACCOUNTANT', 'ORDER_VIEW', 'ALL'),
  ROW('ACCOUNTANT', 'ORDER_APPROVAL_VIEW', 'ALL'),
  ROW('ACCOUNTANT', 'INVENTORY_VIEW', 'ALL'),
  ROW('ACCOUNTANT', 'DELIVERY_VIEW', 'ALL'),
  ROW('ACCOUNTANT', 'INVOICE_VIEW', 'ALL'),
  ROW('ACCOUNTANT', 'INVOICE_MANAGE', 'ALL'),
  ROW('ACCOUNTANT', 'RETURN_VIEW', 'ALL'),
  ROW('ACCOUNTANT', 'RETURN_MANAGE', 'ALL'),
  ROW('ACCOUNTANT', 'REPORT_VIEW', 'ALL'),
  -- Quản lý kinh doanh (Sales Manager)
  ROW('SALES_MANAGER', 'PRODUCT_VIEW', 'ALL'),
  ROW('SALES_MANAGER', 'PRODUCT_MANAGE', 'ALL'),
  ROW('SALES_MANAGER', 'COST_PRICE_VIEW', 'ALL'),
  ROW('SALES_MANAGER', 'CUSTOMER_VIEW', 'ALL'),
  ROW('SALES_MANAGER', 'CUSTOMER_MANAGE', 'ALL'),
  ROW('SALES_MANAGER', 'ORDER_VIEW', 'ALL'),
  ROW('SALES_MANAGER', 'ORDER_MANAGE', 'ALL'),
  ROW('SALES_MANAGER', 'ORDER_APPROVAL_VIEW', 'ALL'),
  ROW('SALES_MANAGER', 'ORDER_APPROVE', 'ALL'),
  ROW('SALES_MANAGER', 'INVENTORY_VIEW', 'ALL'),
  ROW('SALES_MANAGER', 'DELIVERY_VIEW', 'ALL'),
  ROW('SALES_MANAGER', 'INVOICE_VIEW', 'ALL'),
  ROW('SALES_MANAGER', 'RETURN_VIEW', 'ALL'),
  ROW('SALES_MANAGER', 'RETURN_MANAGE', 'ALL'),
  ROW('SALES_MANAGER', 'REPORT_VIEW', 'ALL'),
  ROW('SALES_MANAGER', 'USER_VIEW', 'ALL')
) AS m (role_code, permission_code, data_scope)
JOIN `roles` r ON r.`code` = m.role_code
JOIN `permissions` p ON p.`code` = m.permission_code;

INSERT INTO `role_permissions` (`role_id`, `permission_id`, `data_scope`)
SELECT r.`id`, p.`id`, 'ALL'
FROM `roles` r
JOIN `permissions` p ON p.`code` <> 'COST_PRICE_VIEW'
WHERE r.`code` = 'ADMIN';

COMMIT;
