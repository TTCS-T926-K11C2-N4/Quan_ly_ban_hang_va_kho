package com.oms.controller;

import com.oms.model.AccountForm;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Arrays;
import java.util.List;

// Đọc form Tạo/Sửa tài khoản (account-form.jsp) từ request: trim, chuỗi rỗng -> null
final class AccountFormParser {

    private AccountFormParser() {
    }

    static AccountForm read(HttpServletRequest request) {
        String[] roleValues = request.getParameterValues("roleCodes");
        List<String> roleCodes = roleValues == null ? List.of()
                : Arrays.stream(roleValues).map(String::trim).filter(code -> !code.isEmpty()).distinct().toList();

        return new AccountForm(
                normalize(request.getParameter("fullName")),
                normalize(request.getParameter("username")),
                normalize(request.getParameter("email")),
                normalize(request.getParameter("phone")),
                roleCodes,
                parseId(request.getParameter("warehouseId")),
                parseId(request.getParameter("regionId")));
    }

    static Long parseId(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
