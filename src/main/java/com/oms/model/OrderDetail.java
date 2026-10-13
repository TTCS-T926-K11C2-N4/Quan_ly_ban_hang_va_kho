package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// Chi tiết một đơn hàng cho màn Chi tiết đơn hàng & vòng đời (S4-06): phần đầu đơn, đại lý, điểm giao, dòng hàng,
// lịch sử đổi trạng thái. Ngày giờ lưu theo UTC, các getter *Text đổi sang giờ Việt Nam để hiển thị.
public class OrderDetail {

    private final long id;
    private final String orderNo;
    private final String status;
    private final long version;
    private final long customerId;
    private final String customerCode;
    private final String customerName;
    private final String customerTaxCode;
    private final String customerPhone;
    private final String customerAddress;
    private final String customerGroupName;
    private final String addressLabel;
    private final String deliveryAddress;
    private final String receiverName;
    private final String receiverPhone;
    private final String routeNote;
    private final String warehouseName;
    private final String salesRepName;
    private final String createdByName;
    private final LocalDate requestedDate;
    private final LocalDateTime createdAt;
    private final LocalDateTime submittedAt;
    private final LocalDateTime approvedAt;
    private final LocalDateTime cancelledAt;
    private final String cancelReason;
    private final String cancelledByName;
    private final boolean needsApproval;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private final String note;
    private final List<Item> items;
    private final List<HistoryEntry> history;

    public OrderDetail(long id, String orderNo, String status, long version, long customerId, String customerCode,
                       String customerName, String customerTaxCode, String customerPhone, String customerAddress,
                       String customerGroupName, String addressLabel, String deliveryAddress, String receiverName,
                       String receiverPhone, String routeNote, String warehouseName, String salesRepName,
                       String createdByName, LocalDate requestedDate, LocalDateTime createdAt,
                       LocalDateTime submittedAt, LocalDateTime approvedAt, LocalDateTime cancelledAt,
                       String cancelReason, String cancelledByName, boolean needsApproval, BigDecimal subtotal,
                       BigDecimal discount, BigDecimal total, String note, List<Item> items,
                       List<HistoryEntry> history) {
        this.id = id;
        this.orderNo = orderNo;
        this.status = status;
        this.version = version;
        this.customerId = customerId;
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.customerTaxCode = customerTaxCode;
        this.customerPhone = customerPhone;
        this.customerAddress = customerAddress;
        this.customerGroupName = customerGroupName;
        this.addressLabel = addressLabel;
        this.deliveryAddress = deliveryAddress;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.routeNote = routeNote;
        this.warehouseName = warehouseName;
        this.salesRepName = salesRepName;
        this.createdByName = createdByName;
        this.requestedDate = requestedDate;
        this.createdAt = createdAt;
        this.submittedAt = submittedAt;
        this.approvedAt = approvedAt;
        this.cancelledAt = cancelledAt;
        this.cancelReason = cancelReason;
        this.cancelledByName = cancelledByName;
        this.needsApproval = needsApproval;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.note = note;
        this.items = List.copyOf(items);
        this.history = List.copyOf(history);
    }

    // Thanh tiến trình: các bước chính đã qua / đang ở / chưa tới. Đơn bị huỷ hoặc từ chối chỉ hiện các bước đã qua
    // rồi tới bước rẽ nhánh (màu đỏ). Thời điểm của bước lấy từ lịch sử; đơn tạo trước khi có lịch sử thì lấy từ
    // ngày tạo / ngày gửi / ngày duyệt của đơn.
    public List<Step> getSteps() {
        boolean branch = OrderStatus.isTerminalBranch(status);
        String reached = branch ? lastMainStatus() : status;
        int reachedIndex = Math.max(OrderStatus.STEPS.indexOf(reached), 0);
        List<Step> steps = new ArrayList<>();
        for (int i = 0; i < OrderStatus.STEPS.size(); i++) {
            String code = OrderStatus.STEPS.get(i);
            if (branch && i > reachedIndex) {
                break;
            }
            String state = i < reachedIndex || branch ? "done" : i == reachedIndex ? "current" : "upcoming";
            steps.add(new Step(code, OrderStatus.label(code), state, i <= reachedIndex ? reachedAt(code) : null));
        }
        if (branch) {
            steps.add(new Step(status, OrderStatus.label(status), "branch", reachedAt(status)));
        }
        return steps;
    }

    // Trạng thái chính cuối cùng trước khi đơn rẽ nhánh (huỷ / từ chối)
    private String lastMainStatus() {
        for (int i = history.size() - 1; i >= 0; i--) {
            HistoryEntry entry = history.get(i);
            if (status.equals(entry.getToStatus()) && entry.getFromStatus() != null) {
                return entry.getFromStatus();
            }
        }
        return submittedAt != null ? OrderStatus.PENDING_APPROVAL : OrderStatus.DRAFT;
    }

    private LocalDateTime reachedAt(String code) {
        for (int i = history.size() - 1; i >= 0; i--) {
            if (code.equals(history.get(i).getToStatus())) {
                return history.get(i).getCreatedAt();
            }
        }
        return switch (code) {
            case OrderStatus.DRAFT -> createdAt;
            case OrderStatus.PENDING_APPROVAL -> submittedAt;
            case OrderStatus.APPROVED -> approvedAt;
            case OrderStatus.CANCELLED -> cancelledAt;
            default -> null;
        };
    }

    public boolean isCancellable() {
        return OrderStatus.isCancellable(status);
    }

    public boolean isCancelled() {
        return OrderStatus.CANCELLED.equals(status);
    }

    // Đã qua khâu xuất kho: không huỷ được nữa, phải lập phiếu trả hàng
    public boolean isShipped() {
        return OrderStatus.STEPS.indexOf(status) >= OrderStatus.STEPS.indexOf(OrderStatus.SHIPPED);
    }

    public String getStatusLabel() {
        return OrderStatus.label(status);
    }

    public String getStatusTone() {
        return OrderStatus.tone(status);
    }

    public String getCreatedAtText() {
        return DateTimeUtil.formatDateTime(createdAt);
    }

    public String getRequestedDateText() {
        return requestedDate == null ? "" : DateTimeUtil.formatDay(requestedDate);
    }

    public String getCancelledAtText() {
        return cancelledAt == null ? "" : DateTimeUtil.formatDateTime(cancelledAt);
    }

    public long getId() {
        return id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getStatus() {
        return status;
    }

    public long getVersion() {
        return version;
    }

    public long getCustomerId() {
        return customerId;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerTaxCode() {
        return customerTaxCode;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public String getCustomerGroupName() {
        return customerGroupName;
    }

    public String getAddressLabel() {
        return addressLabel;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public String getRouteNote() {
        return routeNote;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public String getSalesRepName() {
        return salesRepName;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public String getCancelledByName() {
        return cancelledByName;
    }

    public boolean isNeedsApproval() {
        return needsApproval;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public BigDecimal getDiscount() {
        return discount;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getNote() {
        return note;
    }

    public List<Item> getItems() {
        return items;
    }

    public List<HistoryEntry> getHistory() {
        return history;
    }

    // Lịch sử hiển thị mới nhất trước
    public List<HistoryEntry> getHistoryNewestFirst() {
        List<HistoryEntry> newest = new ArrayList<>(history);
        Collections.reverse(newest);
        return newest;
    }

    // Một dòng hàng; số lượng theo đơn vị đã đặt, kèm số quy về đơn vị cơ sở
    public static class Item {
        private final int lineNo;
        private final String sku;
        private final String productName;
        private final String unitName;
        private final String baseUnitName;
        private final BigDecimal qty;
        private final BigDecimal qtyBase;
        private final BigDecimal unitPrice;
        private final BigDecimal discount;
        private final BigDecimal lineTotal;

        public Item(int lineNo, String sku, String productName, String unitName, String baseUnitName, BigDecimal qty,
                    BigDecimal qtyBase, BigDecimal unitPrice, BigDecimal discount, BigDecimal lineTotal) {
            this.lineNo = lineNo;
            this.sku = sku;
            this.productName = productName;
            this.unitName = unitName;
            this.baseUnitName = baseUnitName;
            this.qty = qty;
            this.qtyBase = qtyBase;
            this.unitPrice = unitPrice;
            this.discount = discount;
            this.lineTotal = lineTotal;
        }

        // Có quy đổi (đặt theo Thùng, Lốc...) thì hiện thêm số theo đơn vị cơ sở
        public boolean isConverted() {
            return qty.compareTo(qtyBase) != 0;
        }

        public String getQtyText() {
            return qty.stripTrailingZeros().toPlainString();
        }

        public String getQtyBaseText() {
            return qtyBase.stripTrailingZeros().toPlainString();
        }

        public int getLineNo() {
            return lineNo;
        }

        public String getSku() {
            return sku;
        }

        public String getProductName() {
            return productName;
        }

        public String getUnitName() {
            return unitName;
        }

        public String getBaseUnitName() {
            return baseUnitName;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public BigDecimal getDiscount() {
            return discount;
        }

        public BigDecimal getLineTotal() {
            return lineTotal;
        }
    }

    // Một lần đổi trạng thái (order_status_history, chỉ thêm, không sửa/xoá)
    public static class HistoryEntry {
        private final String fromStatus;
        private final String toStatus;
        private final String note;
        private final LocalDateTime createdAt;
        private final String actorName;

        public HistoryEntry(String fromStatus, String toStatus, String note, LocalDateTime createdAt,
                            String actorName) {
            this.fromStatus = fromStatus;
            this.toStatus = toStatus;
            this.note = note;
            this.createdAt = createdAt;
            this.actorName = actorName;
        }

        public String getToLabel() {
            return OrderStatus.label(toStatus);
        }

        public String getToTone() {
            return OrderStatus.tone(toStatus);
        }

        public String getCreatedAtText() {
            return DateTimeUtil.formatDateTime(createdAt);
        }

        public String getFromStatus() {
            return fromStatus;
        }

        public String getToStatus() {
            return toStatus;
        }

        public String getNote() {
            return note;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public String getActorName() {
            return actorName;
        }
    }

    // Một bước trên thanh tiến trình; state: done | current | upcoming | branch (huỷ / từ chối)
    public static class Step {
        private final String code;
        private final String label;
        private final String state;
        private final LocalDateTime at;

        public Step(String code, String label, String state, LocalDateTime at) {
            this.code = code;
            this.label = label;
            this.state = state;
            this.at = at;
        }

        public String getAtText() {
            return at == null ? "" : DateTimeUtil.formatDateTime(at);
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public String getState() {
            return state;
        }
    }
}
