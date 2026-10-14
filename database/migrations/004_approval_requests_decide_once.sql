-- =====================================================================
-- S4-05 Duyệt đơn: lịch sử duyệt (approval_requests) không sửa, không xoá được.
--  * Mỗi lần gửi duyệt là 1 dòng PENDING; quản lý quyết định đúng một lần (PENDING -> APPROVED | REJECTED | RETURNED).
--  * Đã quyết định thì không đổi được nữa; không xoá dòng nào.
-- Chạy một lần trên CSDL đã tạo từ bản OMS_schema_mysql.sql cũ; CSDL tạo mới từ
-- OMS_schema_mysql.sql hiện tại đã có các trigger này nên KHÔNG cần chạy.
-- Thân trigger có IF...END IF nên CẦN đổi DELIMITER (chạy bằng mysql client).
-- =====================================================================

DELIMITER $$

CREATE TRIGGER trg_approval_requests_decide_once BEFORE UPDATE ON `approval_requests` FOR EACH ROW
BEGIN
  IF OLD.status <> 'PENDING' OR NEW.status = 'PENDING'
     OR NEW.order_id <> OLD.order_id OR NEW.approval_type <> OLD.approval_type
     OR NOT (NEW.violations <=> OLD.violations) OR NEW.requested_by <> OLD.requested_by
     OR NEW.requested_at <> OLD.requested_at THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'approval_requests: chỉ được ghi quyết định một lần cho yêu cầu đang chờ duyệt';
  END IF;
END$$

CREATE TRIGGER trg_approval_requests_no_delete BEFORE DELETE ON `approval_requests` FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'approval_requests là lịch sử duyệt, không được xoá';
END$$

DELIMITER ;
