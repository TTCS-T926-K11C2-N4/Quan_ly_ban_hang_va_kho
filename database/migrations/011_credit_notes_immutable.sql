-- =====================================================================
-- S8-02 Chứng từ điều chỉnh giảm trừ công nợ: chứng từ đã lập không xoá, không sửa nội dung (số tiền, hoá đơn gốc,
-- phiếu trả). Sai sót xử lý bằng chứng từ ngược, không xoá. Chỉ trigger.
-- Chạy một lần trên CSDL tạo từ bản OMS_schema_mysql.sql cũ; CSDL tạo mới từ bản hiện tại đã có các trigger này nên
-- KHÔNG cần chạy. Thân trigger cần đổi DELIMITER (chạy bằng mysql client).
-- =====================================================================

DELIMITER $$
CREATE TRIGGER trg_credit_notes_immutable BEFORE UPDATE ON `credit_notes` FOR EACH ROW
BEGIN
  IF NOT (NEW.credit_note_no <=> OLD.credit_note_no) OR NEW.invoice_id <> OLD.invoice_id
     OR NOT (NEW.return_id <=> OLD.return_id) OR NEW.customer_id <> OLD.customer_id
     OR NEW.total_amount <> OLD.total_amount THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'credit_notes: chứng từ điều chỉnh đã lập không được sửa nội dung';
  END IF;
END$$
CREATE TRIGGER trg_credit_notes_no_delete BEFORE DELETE ON `credit_notes` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'credit_notes: chứng từ điều chỉnh không được xoá'$$
CREATE TRIGGER trg_credit_note_items_no_update BEFORE UPDATE ON `credit_note_items` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'credit_note_items: dòng chứng từ điều chỉnh không được sửa'$$
CREATE TRIGGER trg_credit_note_items_no_delete BEFORE DELETE ON `credit_note_items` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'credit_note_items: dòng chứng từ điều chỉnh không được xoá'$$
DELIMITER ;
