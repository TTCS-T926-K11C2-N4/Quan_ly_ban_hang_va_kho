package com.oms.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Dữ liệu form Tạo/Sửa phiếu nhập kho (S5-04), giữ nguyên chuỗi người dùng nhập để hiện lại khi có lỗi.
// receiptId null khi tạo mới; adjustsReceiptId khác null khi là phiếu điều chỉnh của một phiếu đã ghi sổ.
public class GoodsReceiptForm {

    private final String receiptId;
    private final String adjustsReceiptId;
    private final String supplierId;
    private final String warehouseId;
    private final String receiptDate;
    private final String documentRef;
    private final String note;
    private final List<Line> lines;

    public GoodsReceiptForm(String receiptId, String adjustsReceiptId, String supplierId, String warehouseId,
                            String receiptDate, String documentRef, String note, List<Line> lines) {
        this.receiptId = receiptId;
        this.adjustsReceiptId = adjustsReceiptId;
        this.supplierId = supplierId;
        this.warehouseId = warehouseId;
        this.receiptDate = receiptDate;
        this.documentRef = documentRef;
        this.note = note;
        this.lines = List.copyOf(lines);
    }

    public String getReceiptId() {
        return receiptId;
    }

    public String getAdjustsReceiptId() {
        return adjustsReceiptId;
    }

    public boolean isAdjustment() {
        return adjustsReceiptId != null;
    }

    public String getSupplierId() {
        return supplierId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getReceiptDate() {
        return receiptDate;
    }

    public String getDocumentRef() {
        return documentRef;
    }

    public String getNote() {
        return note;
    }

    public List<Line> getLines() {
        return lines;
    }

    // Một dòng hàng nhập; errors theo tên ô của dòng (productId, unitId, qty, unitPrice, locationId, lotNo, expiryDate)
    public static class Line {
        private final String productId;
        private final String productText;
        private final String unitId;
        private final String qty;
        private final String unitPrice;
        private final String locationId;
        private final String lotNo;
        private final String expiryDate;
        private final Map<String, String> errors = new LinkedHashMap<>();

        public Line(String productId, String productText, String unitId, String qty, String unitPrice,
                    String locationId, String lotNo, String expiryDate) {
            this.productId = productId;
            this.productText = productText;
            this.unitId = unitId;
            this.qty = qty;
            this.unitPrice = unitPrice;
            this.locationId = locationId;
            this.lotNo = lotNo;
            this.expiryDate = expiryDate;
        }

        public String getProductId() {
            return productId;
        }

        public String getProductText() {
            return productText;
        }

        public String getUnitId() {
            return unitId;
        }

        public String getQty() {
            return qty;
        }

        public String getUnitPrice() {
            return unitPrice;
        }

        public String getLocationId() {
            return locationId;
        }

        public String getLotNo() {
            return lotNo;
        }

        public String getExpiryDate() {
            return expiryDate;
        }

        public Map<String, String> getErrors() {
            return errors;
        }

        // Dòng trống hoàn toàn (vd bấm "Thêm dòng hàng" rồi để đó) thì bỏ qua khi lưu
        public boolean isBlank() {
            return productId == null && productText == null && qty == null && unitPrice == null && lotNo == null;
        }
    }
}
