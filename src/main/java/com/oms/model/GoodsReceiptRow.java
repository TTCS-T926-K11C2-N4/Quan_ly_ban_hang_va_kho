package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// Một dòng ở danh sách phiếu nhập kho (S5-04). Phiếu nháp không ảnh hưởng tồn; ghi sổ (CONFIRMED) mới cộng tồn.
public class GoodsReceiptRow {

    public static final String DRAFT = "DRAFT";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String CANCELLED = "CANCELLED";

    // Trạng thái chọn được ở bộ lọc, theo thứ tự hiện
    public static final Map<String, String> STATUS_LABELS;

    static {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put(DRAFT, "Nháp");
        labels.put(CONFIRMED, "Đã ghi sổ");
        STATUS_LABELS = Collections.unmodifiableMap(labels);
    }

    private final long id;
    private final String receiptNo;
    private final String supplierName;
    private final String warehouseCode;
    private final String warehouseName;
    private final LocalDate receiptDate;
    private final BigDecimal totalAmount;
    private final String status;
    private final String adjustsReceiptNo;

    public GoodsReceiptRow(long id, String receiptNo, String supplierName, String warehouseCode, String warehouseName,
                           LocalDate receiptDate, BigDecimal totalAmount, String status, String adjustsReceiptNo) {
        this.id = id;
        this.receiptNo = receiptNo;
        this.supplierName = supplierName;
        this.warehouseCode = warehouseCode;
        this.warehouseName = warehouseName;
        this.receiptDate = receiptDate;
        this.totalAmount = totalAmount;
        this.status = status;
        this.adjustsReceiptNo = adjustsReceiptNo;
    }

    public static String statusLabel(String status) {
        return CANCELLED.equals(status) ? "Đã huỷ" : STATUS_LABELS.getOrDefault(status, status);
    }

    public long getId() {
        return id;
    }

    public String getReceiptNo() {
        return receiptNo;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public LocalDate getReceiptDate() {
        return receiptDate;
    }

    public String getReceiptDateText() {
        return DateTimeUtil.formatDay(receiptDate);
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return statusLabel(status);
    }

    public boolean isDraft() {
        return DRAFT.equals(status);
    }

    // Mã phiếu gốc nếu đây là phiếu điều chỉnh; null với phiếu nhập thường
    public String getAdjustsReceiptNo() {
        return adjustsReceiptNo;
    }
}
