package com.oms.model;

// Nhà cung cấp (S2-09). Dùng cho cả dòng danh sách lẫn dữ liệu form: id = null khi đang tạo mới.
public class Supplier {

    public static final String ACTIVE = "ACTIVE";
    public static final String INACTIVE = "INACTIVE";

    private final Long id;
    private final String code;
    private final String name;
    private final String taxCode;
    private final String contactName;
    private final String phone;
    private final String email;
    private final String address;
    private final String paymentTerms;
    private final String status;
    private final boolean hasReceipts;

    public Supplier(Long id, String code, String name, String taxCode, String contactName, String phone, String email,
                    String address, String paymentTerms, String status, boolean hasReceipts) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.taxCode = taxCode;
        this.contactName = contactName;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.paymentTerms = paymentTerms;
        this.status = status;
        this.hasReceipts = hasReceipts;
    }

    public Long getId() {
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

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getAddress() {
        return address;
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

    // Đã có phiếu nhập kho: không xoá được, chỉ ngừng giao dịch (S2-09)
    public boolean isHasReceipts() {
        return hasReceipts;
    }

    public boolean isDeletable() {
        return !hasReceipts;
    }
}
