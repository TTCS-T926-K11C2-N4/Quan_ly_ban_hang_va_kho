package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// Hoá đơn bán hàng nội bộ (S7-01): sinh từ số lượng thực giao của các phiếu xuất đã giao, mỗi dòng truy về một dòng
// phiếu xuất. Đã phát hành thì không sửa, không xoá (trigger CSDL); điều chỉnh bằng chứng từ điều chỉnh (S8-02).
public class Invoice {

    public static final String ISSUED = "ISSUED";
    public static final String PARTIALLY_PAID = "PARTIALLY_PAID";
    public static final String PAID = "PAID";
    public static final String CANCELLED = "CANCELLED";

    private static final Map<String, String> LABELS = Map.of(ISSUED, "Chưa thanh toán",
            PARTIALLY_PAID, "Thanh toán một phần", PAID, "Đã thanh toán", CANCELLED, "Đã huỷ");
    private static final Map<String, String> TONES = Map.of(ISSUED, "warning", PARTIALLY_PAID, "info",
            PAID, "success", CANCELLED, "danger");

    private final long id;
    private final String invoiceNo;
    private final long customerId;
    private final String customerCode;
    private final String customerName;
    private final String customerTaxCode;
    private final String customerAddress;
    private final LocalDate issueDate;
    private final LocalDate dueDate;
    private final int paymentTermDays;
    private final String status;
    private final BigDecimal subtotal;
    private final BigDecimal discount;
    private final BigDecimal total;
    private final BigDecimal paid;
    private final BigDecimal credited;
    private final BigDecimal outstanding;
    private final String issuedByName;
    private final LocalDateTime issuedAt;
    private final String note;
    private final List<Item> items;

    public Invoice(long id, String invoiceNo, long customerId, String customerCode, String customerName,
                   String customerTaxCode, String customerAddress, LocalDate issueDate, LocalDate dueDate,
                   int paymentTermDays, String status, BigDecimal subtotal, BigDecimal discount, BigDecimal total,
                   BigDecimal paid, BigDecimal credited, BigDecimal outstanding, String issuedByName,
                   LocalDateTime issuedAt, String note, List<Item> items) {
        this.id = id;
        this.invoiceNo = invoiceNo;
        this.customerId = customerId;
        this.customerCode = customerCode;
        this.customerName = customerName;
        this.customerTaxCode = customerTaxCode;
        this.customerAddress = customerAddress;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.paymentTermDays = paymentTermDays;
        this.status = status;
        this.subtotal = subtotal;
        this.discount = discount;
        this.total = total;
        this.paid = paid;
        this.credited = credited;
        this.outstanding = outstanding;
        this.issuedByName = issuedByName;
        this.issuedAt = issuedAt;
        this.note = note;
        this.items = List.copyOf(items);
    }

    public long getId() {
        return id;
    }

    public String getInvoiceNo() {
        return invoiceNo;
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

    public String getCustomerAddress() {
        return customerAddress;
    }

    public String getIssueDateText() {
        return DateTimeUtil.formatDay(issueDate);
    }

    public String getDueDateText() {
        return DateTimeUtil.formatDay(dueDate);
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public int getPaymentTermDays() {
        return paymentTermDays;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return LABELS.getOrDefault(status, status);
    }

    public String getStatusTone() {
        return TONES.getOrDefault(status, "neutral");
    }

    // Còn nợ và đã qua hạn thanh toán
    public boolean isOverdue() {
        return outstanding.signum() > 0 && dueDate.isBefore(DateTimeUtil.today())
                && (ISSUED.equals(status) || PARTIALLY_PAID.equals(status));
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

    public BigDecimal getPaid() {
        return paid;
    }

    public BigDecimal getCredited() {
        return credited;
    }

    public BigDecimal getOutstanding() {
        return outstanding;
    }

    public String getIssuedByName() {
        return issuedByName;
    }

    public String getIssuedAtText() {
        return DateTimeUtil.formatDateTime(issuedAt);
    }

    public String getNote() {
        return note;
    }

    public List<Item> getItems() {
        return items;
    }

    // Dòng hoá đơn
    public static class Item {
        private final int lineNo;
        private final String sku;
        private final String productName;
        private final String unitName;
        private final BigDecimal qty;
        private final BigDecimal unitPrice;
        private final BigDecimal discount;
        private final BigDecimal lineTotal;
        private final String deliveryNo;
        private final String orderNo;

        public Item(int lineNo, String sku, String productName, String unitName, BigDecimal qty, BigDecimal unitPrice,
                    BigDecimal discount, BigDecimal lineTotal, String deliveryNo, String orderNo) {
            this.lineNo = lineNo;
            this.sku = sku;
            this.productName = productName;
            this.unitName = unitName;
            this.qty = qty;
            this.unitPrice = unitPrice;
            this.discount = discount;
            this.lineTotal = lineTotal;
            this.deliveryNo = deliveryNo;
            this.orderNo = orderNo;
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

        public BigDecimal getQty() {
            return qty;
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

        public String getDeliveryNo() {
            return deliveryNo;
        }

        public String getOrderNo() {
            return orderNo;
        }
    }
}
