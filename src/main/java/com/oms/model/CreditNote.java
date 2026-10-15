package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Chứng từ điều chỉnh giảm trừ công nợ sinh từ phiếu trả hàng đã nhận (S8-02); giữ liên kết hoá đơn gốc
public class CreditNote {

    private final long id;
    private final String creditNoteNo;
    private final long invoiceId;
    private final String invoiceNo;
    private final String returnNo;
    private final String customerName;
    private final BigDecimal total;
    private final String reason;
    private final String issuedByName;
    private final LocalDateTime issuedAt;
    private final List<Item> items;

    public CreditNote(long id, String creditNoteNo, long invoiceId, String invoiceNo, String returnNo,
                      String customerName, BigDecimal total, String reason, String issuedByName,
                      LocalDateTime issuedAt, List<Item> items) {
        this.id = id;
        this.creditNoteNo = creditNoteNo;
        this.invoiceId = invoiceId;
        this.invoiceNo = invoiceNo;
        this.returnNo = returnNo;
        this.customerName = customerName;
        this.total = total;
        this.reason = reason;
        this.issuedByName = issuedByName;
        this.issuedAt = issuedAt;
        this.items = items;
    }

    public CreditNote withItems(List<Item> newItems) {
        return new CreditNote(id, creditNoteNo, invoiceId, invoiceNo, returnNo, customerName, total, reason,
                issuedByName, issuedAt, newItems);
    }

    public long getId() {
        return id;
    }

    public String getCreditNoteNo() {
        return creditNoteNo;
    }

    public long getInvoiceId() {
        return invoiceId;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public String getReturnNo() {
        return returnNo;
    }

    public String getCustomerName() {
        return customerName;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public String getReason() {
        return reason;
    }

    public String getIssuedByName() {
        return issuedByName;
    }

    public String getIssuedAtText() {
        return DateTimeUtil.formatDateTime(issuedAt);
    }

    public List<Item> getItems() {
        return items;
    }

    // Một dòng giảm trừ: số lượng theo đơn vị cơ sở, đơn giá bình quân theo hoá đơn gốc
    public static class Item {
        private final String sku;
        private final String productName;
        private final BigDecimal qtyBase;
        private final BigDecimal unitPrice;
        private final BigDecimal amount;

        public Item(String sku, String productName, BigDecimal qtyBase, BigDecimal unitPrice, BigDecimal amount) {
            this.sku = sku;
            this.productName = productName;
            this.qtyBase = qtyBase;
            this.unitPrice = unitPrice;
            this.amount = amount;
        }

        public String getSku() {
            return sku;
        }

        public String getProductName() {
            return productName;
        }

        public BigDecimal getQtyBase() {
            return qtyBase;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public BigDecimal getAmount() {
            return amount;
        }
    }
}
