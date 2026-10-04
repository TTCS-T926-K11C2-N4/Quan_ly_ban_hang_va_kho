package com.oms.model;

// Dữ liệu form Thêm/Sửa nhà cung cấp, giữ nguyên chuỗi người dùng nhập để hiện lại khi có lỗi
public class SupplierForm {

    private final String code;
    private final String name;
    private final String taxCode;
    private final String contactName;
    private final String paymentTerms;
    private final String status;

    public SupplierForm(String code, String name, String taxCode, String contactName, String paymentTerms,
                        String status) {
        this.code = code;
        this.name = name;
        this.taxCode = taxCode;
        this.contactName = contactName;
        this.paymentTerms = paymentTerms;
        this.status = status;
    }

    // Form trống khi thêm mới: mặc định đang hoạt động
    public static SupplierForm empty() {
        return new SupplierForm(null, null, null, null, null, Supplier.ACTIVE);
    }

    public static SupplierForm of(Supplier supplier) {
        return new SupplierForm(supplier.getCode(), supplier.getName(), supplier.getTaxCode(),
                supplier.getContactName(), supplier.getPaymentTerms(), supplier.getStatus());
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
}
