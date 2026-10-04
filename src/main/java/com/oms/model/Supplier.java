package com.oms.model;

// Nhà cung cấp (S2-09). hasReceipts = đã có phiếu nhập (hoặc lô hàng) gắn với nhà cung cấp: không xoá được,
// chỉ ngừng giao dịch.
public class Supplier {

    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";

    private final long id;
    private final String code;
    private final String name;
    private final String taxCode;
    private final String contactName;
    private final String paymentTerms;
    private final String status;
    private final boolean hasReceipts;

    public Supplier(long id, String code, String name, String taxCode, String contactName, String paymentTerms,
                    String status, boolean hasReceipts) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.taxCode = taxCode;
        this.contactName = contactName;
        this.paymentTerms = paymentTerms;
        this.status = status;
        this.hasReceipts = hasReceipts;
    }

    public long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getTaxCode() {
        return taxCode;
    }

    public String getContactName() {
        return contactName;
    }

    public String getPaymentTerms() {
        return paymentTerms;
    }

    public String getStatus() {
        return status;
    }

    public boolean isActive() {
        return ACTIVE.equals(status);
    }

    public boolean isHasReceipts() {
        return hasReceipts;
    }
}
