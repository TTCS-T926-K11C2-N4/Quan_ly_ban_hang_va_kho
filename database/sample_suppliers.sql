-- =====================================================================
-- Nhà cung cấp mẫu (S2-09) để thử màn hình Nhà cung cấp, theo dữ liệu trong thiết kế.
-- Chạy lại lần 2 không lỗi: nhà cung cấp đã có mã thì bỏ qua (INSERT IGNORE theo cột code UNIQUE).
-- Chưa có phiếu nhập nên cả 5 đều xoá được; nút "Ngừng giao dịch" chỉ hiện khi đã có phiếu nhập (SCRUM-73).
-- =====================================================================

SET NAMES utf8mb4;

INSERT IGNORE INTO `suppliers` (`code`, `name`, `tax_code`, `contact_name`, `payment_terms`, `status`,
                                `created_at`, `updated_at`)
VALUES
  ('NCC001', 'Công ty TNHH Thương mại An Phát', '0101234567', 'Nguyễn Văn A', 'TT 30 ngày', 'ACTIVE', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  ('NCC002', 'Công ty CP Vật tư Hoàng Gia', '0109876543', 'Trần Thị B', 'TT 15 ngày', 'ACTIVE', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  ('NCC003', 'Công ty TNHH Kim Long', '0312345678', 'Lê Văn C', 'TT 45 ngày', 'INACTIVE', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  ('NCC004', 'Công ty CP Thực phẩm Việt', '0405678901', 'Phạm Thị D', 'TT 30 ngày', 'ACTIVE', UTC_TIMESTAMP(), UTC_TIMESTAMP()),
  ('NCC005', 'Công ty TNHH Điện máy Minh Tâm', '0108765432', 'Hoàng Văn E', 'TT 60 ngày', 'ACTIVE', UTC_TIMESTAMP(), UTC_TIMESTAMP());
