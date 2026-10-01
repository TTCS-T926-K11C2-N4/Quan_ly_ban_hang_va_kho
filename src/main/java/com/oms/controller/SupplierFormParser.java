package com.oms.controller;

import com.oms.model.Supplier;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;

// Đọc form Thêm/Sửa nhà cung cấp từ request: trim, chuỗi rỗng -> null
final class SupplierFormParser {

    private SupplierFormParser() {
    }

    // id, trạng thái và "đã có phiếu nhập" không lấy từ form mà do Servlet truyền vào từ DB
    static Supplier read(HttpServletRequest request, Long id, String status, boolean hasReceipts) {
        String code = normalize(request.getParameter("code"));
        String phone = normalize(request.getParameter("phone"));
        return new Supplier(id,
                code == null ? null : code.toUpperCase(Locale.ROOT),
                normalize(request.getParameter("name")),
                normalize(request.getParameter("taxCode")),
                normalize(request.getParameter("contactName")),
                // "024 3456 7890", "0912.345.678" -> chỉ giữ chữ số và dấu +
                phone == null ? null : phone.replaceAll("[\\s.\\-]", ""),
                normalize(request.getParameter("email")),
                normalize(request.getParameter("address")),
                normalize(request.getParameter("paymentTerms")),
                status,
                hasReceipts);
    }

    static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
