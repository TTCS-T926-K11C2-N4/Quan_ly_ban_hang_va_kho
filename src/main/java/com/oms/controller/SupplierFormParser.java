package com.oms.controller;

import com.oms.model.SupplierForm;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;

// Đọc form Thêm/Sửa nhà cung cấp (S2-09): bỏ khoảng trắng thừa, ô trống thành null, mã viết hoa
final class SupplierFormParser {

    private SupplierFormParser() {
    }

    static SupplierForm parse(HttpServletRequest request) {
        String code = normalize(request.getParameter("code"));
        return new SupplierForm(code == null ? null : code.toUpperCase(Locale.ROOT), normalize(request.getParameter("name")),
                normalize(request.getParameter("taxCode")), normalize(request.getParameter("contactName")),
                normalize(request.getParameter("paymentTerms")), normalize(request.getParameter("status")));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
