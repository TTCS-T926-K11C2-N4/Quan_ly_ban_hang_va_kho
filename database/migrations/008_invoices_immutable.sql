-- =====================================================================
-- S7-01 Hoá đơn: hoá đơn đã phát hành không sửa nội dung và không xoá (chỉ lập chứng từ điều chỉnh).
-- Chỉ trigger; nội dung hoá đơn và dòng hoá đơn bị chặn sửa / xoá, các trường thanh toán (amount_paid, amount_credited,
-- status, version) vẫn cập nhật được khi ghi nhận thu tiền (S7-02).
-- Chạy một lần trên CSDL đã tạo từ bản OMS_schema_mysql.sql cũ; CSDL tạo mới từ OMS_schema_mysql.sql hiện tại
-- đã có các trigger này nên KHÔNG cần chạy. Thân trigger cần đổi DELIMITER (chạy bằng mysql client).
-- =====================================================================

DELIMITER $$
-- S7-01: hoá đơn đã phát hành không sửa nội dung và không xoá; chỉ được đổi các trường thanh toán, trạng thái, phiên bản
CREATE TRIGGER trg_invoices_immutable BEFORE UPDATE ON `invoices` FOR EACH ROW
BEGIN
  IF NOT (NEW.invoice_no <=> OLD.invoice_no) OR NEW.customer_id <> OLD.customer_id OR NEW.issue_date <> OLD.issue_date
     OR NEW.due_date <> OLD.due_date OR NEW.payment_term_days <> OLD.payment_term_days
     OR NEW.subtotal_amount <> OLD.subtotal_amount OR NEW.discount_amount <> OLD.discount_amount
     OR NEW.total_amount <> OLD.total_amount THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'invoices: hoá đơn đã phát hành không được sửa nội dung, chỉ lập chứng từ điều chỉnh';
  END IF;
END$$
CREATE TRIGGER trg_invoices_no_delete BEFORE DELETE ON `invoices` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'invoices: hoá đơn không được xoá'$$
CREATE TRIGGER trg_invoice_items_no_update BEFORE UPDATE ON `invoice_items` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'invoice_items: dòng hoá đơn không được sửa'$$
CREATE TRIGGER trg_invoice_items_no_delete BEFORE DELETE ON `invoice_items` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'invoice_items: dòng hoá đơn không được xoá'$$

DELIMITER ;
