package com.oms.controller;

import com.oms.model.GoodsReceiptDetail;
import com.oms.model.GoodsReceiptFilter;
import com.oms.model.GoodsReceiptForm;
import com.oms.util.MoneyUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.util.ArrayList;
import java.util.List;

// Đọc form và bộ lọc phiếu nhập kho (S5-04). Mỗi dòng hàng gửi đủ các ô (kể cả ô lô đang ẩn) nên các mảng
// tham số cùng độ dài, phần tử thứ i là của dòng thứ i.
final class GoodsReceiptFormParser {

    private GoodsReceiptFormParser() {
    }

    static GoodsReceiptForm parse(HttpServletRequest request, String receiptId, String adjustsReceiptId) {
        String[] productIds = values(request, "productId");
        List<GoodsReceiptForm.Line> lines = new ArrayList<>();
        for (int i = 0; i < productIds.length; i++) {
            lines.add(new GoodsReceiptForm.Line(normalize(productIds[i]), at(request, "productText", i),
                    at(request, "unitId", i), at(request, "qty", i), at(request, "unitPrice", i),
                    at(request, "locationId", i), at(request, "lotNo", i), at(request, "expiryDate", i)));
        }
        return new GoodsReceiptForm(receiptId, adjustsReceiptId, normalize(request.getParameter("supplierId")),
                normalize(request.getParameter("warehouseId")), normalize(request.getParameter("receiptDate")),
                normalize(request.getParameter("documentRef")), normalize(request.getParameter("note")), lines);
    }

    // Mở lại phiếu nháp để sửa: đơn giá hiện kiểu 120.000 như người dùng gõ
    static GoodsReceiptForm of(GoodsReceiptDetail receipt) {
        List<GoodsReceiptForm.Line> lines = new ArrayList<>();
        for (GoodsReceiptDetail.Item item : receipt.getItems()) {
            lines.add(new GoodsReceiptForm.Line(Long.toString(item.getProductId()), item.getSku() + " - "
                    + item.getProductName(), Long.toString(item.getUnitId()), item.getQtyText(),
                    MoneyUtil.format(item.getUnitPrice()),
                    item.getLocationId() == null ? null : item.getLocationId().toString(), item.getLotNo(),
                    item.getExpiryDate() == null ? null : item.getExpiryDate().toString()));
        }
        return new GoodsReceiptForm(Long.toString(receipt.getId()),
                receipt.getAdjustsReceiptId() == null ? null : receipt.getAdjustsReceiptId().toString(),
                Long.toString(receipt.getSupplierId()), Long.toString(receipt.getWarehouseId()),
                receipt.getReceiptDate().toString(), receipt.getDocumentRef(), receipt.getNote(), lines);
    }

    static GoodsReceiptFilter parseFilter(HttpServletRequest request) {
        return new GoodsReceiptFilter(normalize(request.getParameter("keyword")), normalize(request.getParameter("status")),
                AccountFormParser.parseId(request.getParameter("warehouse")), normalize(request.getParameter("date")));
    }

    static int parsePage(HttpServletRequest request) {
        try {
            String page = request.getParameter("page");
            return page == null ? 1 : Integer.parseInt(page);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private static String[] values(HttpServletRequest request, String name) {
        String[] values = request.getParameterValues(name);
        return values == null ? new String[0] : values;
    }

    private static String at(HttpServletRequest request, String name, int index) {
        String[] values = values(request, name);
        return index < values.length ? normalize(values[index]) : null;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
