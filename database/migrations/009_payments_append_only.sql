-- =====================================================================
-- S7-02 Ghi nhận thanh toán: phiếu thu và dòng đối trừ đã ghi nhận không xoá; nội dung không sửa.
-- Chỉ trigger. Phiếu thu chỉ được đổi trạng thái/xác nhận/huỷ (các luồng S7-06, S7-09 về sau); số tiền, đại lý, ngày thu,
-- người thu, số phiếu bị chặn sửa. Sai sót xử lý bằng bút toán điều chỉnh, không xoá.
-- Chạy một lần trên CSDL tạo từ bản OMS_schema_mysql.sql cũ; thân trigger cần đổi DELIMITER (chạy bằng mysql client).
-- =====================================================================

DELIMITER $$
CREATE TRIGGER trg_payments_immutable BEFORE UPDATE ON `payments` FOR EACH ROW
BEGIN
  IF NOT (NEW.receipt_no <=> OLD.receipt_no) OR NEW.customer_id <> OLD.customer_id OR NEW.amount <> OLD.amount
     OR NEW.payment_date <> OLD.payment_date OR NEW.collected_by <> OLD.collected_by OR NEW.method <> OLD.method THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'payments: phiếu thu đã ghi nhận không được sửa, chỉ lập bút toán điều chỉnh';
  END IF;
END$$
CREATE TRIGGER trg_payments_no_delete BEFORE DELETE ON `payments` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'payments: phiếu thu không được xoá'$$
CREATE TRIGGER trg_payment_allocations_no_update BEFORE UPDATE ON `payment_allocations` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'payment_allocations: dòng đối trừ không được sửa'$$
CREATE TRIGGER trg_payment_allocations_no_delete BEFORE DELETE ON `payment_allocations` FOR EACH ROW
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'payment_allocations: dòng đối trừ không được xoá'$$
DELIMITER ;
