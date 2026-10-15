package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

// Một dòng thẻ kho (S6-02): một phát sinh làm đổi tồn của SKU tại một kho, kèm tồn sau phát sinh và chứng từ gốc
public class StockCardEntry {

    private static final Map<String, String> TYPE_LABELS = Map.of(
            "RECEIPT", "Nhập kho", "ISSUE", "Xuất kho", "TRANSFER_OUT", "Chuyển đi", "TRANSFER_IN", "Chuyển đến",
            "ADJUSTMENT", "Điều chỉnh", "STOCKTAKE", "Kiểm kê", "RETURN_IN", "Nhập hàng trả");
    private static final Map<String, String> DOC_LABELS = Map.of(
            "GOODS_RECEIPT", "Phiếu nhập kho", "DELIVERY_NOTE", "Phiếu xuất kho",
            "STOCK_TRANSFER", "Phiếu chuyển kho", "STOCKTAKE", "Phiếu kiểm kê");
    // Màn xem chứng từ gốc theo loại chứng từ (đường dẫn trong ứng dụng, tham số id)
    private static final Map<String, String> DOC_PATHS = Map.of(
            "GOODS_RECEIPT", "/goods-receipts/view", "DELIVERY_NOTE", "/delivery-notes/view",
            "STOCK_TRANSFER", "/transfers/view", "STOCKTAKE", "/stocktakes/view");

    private final LocalDateTime occurredAt;
    private final String warehouseCode;
    private final String lotNo;
    private final String movementType;
    private final BigDecimal qtyChange;
    private final BigDecimal balanceAfter;
    private final String refType;
    private final long refId;
    private final String documentNo;
    private final String note;
    private final String actorName;

    public StockCardEntry(LocalDateTime occurredAt, String warehouseCode, String lotNo, String movementType,
                          BigDecimal qtyChange, BigDecimal balanceAfter, String refType, long refId,
                          String documentNo, String note, String actorName) {
        this.occurredAt = occurredAt;
        this.warehouseCode = warehouseCode;
        this.lotNo = lotNo;
        this.movementType = movementType;
        this.qtyChange = qtyChange;
        this.balanceAfter = balanceAfter;
        this.refType = refType;
        this.refId = refId;
        this.documentNo = documentNo;
        this.note = note;
        this.actorName = actorName;
    }

    public String getOccurredAtText() {
        return DateTimeUtil.formatDateTime(occurredAt);
    }

    public String getWarehouseCode() {
        return warehouseCode;
    }

    public String getLotNo() {
        return lotNo;
    }

    public String getMovementType() {
        return movementType;
    }

    public String getMovementLabel() {
        return TYPE_LABELS.getOrDefault(movementType, movementType);
    }

    public BigDecimal getQtyChange() {
        return qtyChange;
    }

    public boolean isIncrease() {
        return qtyChange.signum() > 0;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getRefType() {
        return refType;
    }

    public long getRefId() {
        return refId;
    }

    public String getDocumentLabel() {
        return DOC_LABELS.getOrDefault(refType, refType);
    }

    // Mã chứng từ gốc; chứng từ không còn trong bảng nguồn thì ghi "#id"
    public String getDocumentNo() {
        return documentNo == null ? "#" + refId : documentNo;
    }

    // null nếu chưa có màn xem loại chứng từ này
    public String getDocumentPath() {
        return DOC_PATHS.get(refType);
    }

    public String getNote() {
        return note;
    }

    public String getActorName() {
        return actorName;
    }
}
