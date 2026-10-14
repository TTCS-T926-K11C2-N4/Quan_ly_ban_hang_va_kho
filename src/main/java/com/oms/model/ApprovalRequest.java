package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Một lần gửi duyệt đơn (approval_requests, S4-05): lý do cần duyệt (vi phạm chụp lại lúc gửi) và quyết định của
// quản lý. Quyết định chỉ ghi một lần, sau đó không sửa, không xoá (trigger trg_approval_requests_decide_once).
public class ApprovalRequest {

    public static final String PENDING = "PENDING";
    public static final String APPROVED = "APPROVED";
    public static final String REJECTED = "REJECTED";
    public static final String RETURNED = "RETURNED";

    private static final Map<String, String> LABELS = Map.of(PENDING, "Chờ duyệt", APPROVED, "Đã duyệt",
            REJECTED, "Từ chối", RETURNED, "Trả lại sửa");
    private static final Map<String, String> TONES = Map.of(PENDING, "pending-approval", APPROVED, "approved",
            REJECTED, "rejected", RETURNED, "draft");

    private final long id;
    private final String status;
    private final List<Violation> violations;
    private final LocalDateTime requestedAt;
    private final String requestedByName;
    private final LocalDateTime decidedAt;
    private final String decidedByName;
    private final String comment;

    public ApprovalRequest(long id, String status, List<Violation> violations, LocalDateTime requestedAt,
                           String requestedByName, LocalDateTime decidedAt, String decidedByName, String comment) {
        this.id = id;
        this.status = status;
        this.violations = List.copyOf(violations);
        this.requestedAt = requestedAt;
        this.requestedByName = requestedByName;
        this.decidedAt = decidedAt;
        this.decidedByName = decidedByName;
        this.comment = comment;
    }

    // Vi phạm khiến đơn phải duyệt. Vượt hạn mức: exceededBy, creditLimit, used (công nợ + đơn chưa xuất hoá đơn
    // trước đơn này). Dưới giá sàn: lineNo, sku, unitPrice (giá bán theo đơn vị đặt), netPrice và floorPrice tính
    // theo đơn vị đặt sau chiết khấu. Dùng class có getter vì EL của Tomcat 10.1 chưa đọc được thuộc tính của record.
    public static final class Violation {

        public static final String OVER_CREDIT_LIMIT = "OVER_CREDIT_LIMIT";
        public static final String BELOW_FLOOR_PRICE = "BELOW_FLOOR_PRICE";

        private final String type;
        private final BigDecimal exceededBy;
        private final BigDecimal creditLimit;
        private final BigDecimal used;
        private final Integer lineNo;
        private final String sku;
        private final BigDecimal unitPrice;
        private final BigDecimal netPrice;
        private final BigDecimal floorPrice;

        public Violation(String type, BigDecimal exceededBy, BigDecimal creditLimit, BigDecimal used,
                         Integer lineNo, String sku, BigDecimal unitPrice, BigDecimal netPrice, BigDecimal floorPrice) {
            this.type = type;
            this.exceededBy = exceededBy;
            this.creditLimit = creditLimit;
            this.used = used;
            this.lineNo = lineNo;
            this.sku = sku;
            this.unitPrice = unitPrice;
            this.netPrice = netPrice;
            this.floorPrice = floorPrice;
        }

        public boolean isOverCreditLimit() {
            return OVER_CREDIT_LIMIT.equals(type);
        }

        public boolean isBelowFloorPrice() {
            return BELOW_FLOOR_PRICE.equals(type);
        }

        public String getType() {
            return type;
        }

        public BigDecimal getExceededBy() {
            return exceededBy;
        }

        public BigDecimal getCreditLimit() {
            return creditLimit;
        }

        public BigDecimal getUsed() {
            return used;
        }

        public Integer getLineNo() {
            return lineNo;
        }

        public String getSku() {
            return sku;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public BigDecimal getNetPrice() {
            return netPrice;
        }

        public BigDecimal getFloorPrice() {
            return floorPrice;
        }
    }

    public boolean isPending() {
        return PENDING.equals(status);
    }

    public Violation getCreditViolation() {
        return violations.stream().filter(Violation::isOverCreditLimit).findFirst().orElse(null);
    }

    public List<Violation> getFloorViolations() {
        return violations.stream().filter(Violation::isBelowFloorPrice).toList();
    }

    public String getStatusLabel() {
        return LABELS.getOrDefault(status, status);
    }

    public String getStatusTone() {
        return TONES.getOrDefault(status, "draft");
    }

    public String getRequestedAtText() {
        return DateTimeUtil.formatDateTime(requestedAt);
    }

    public String getDecidedAtText() {
        return DateTimeUtil.formatDateTime(decidedAt);
    }

    public long getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public List<Violation> getViolations() {
        return violations;
    }

    public String getRequestedByName() {
        return requestedByName;
    }

    public String getDecidedByName() {
        return decidedByName;
    }

    public String getComment() {
        return comment;
    }
}
