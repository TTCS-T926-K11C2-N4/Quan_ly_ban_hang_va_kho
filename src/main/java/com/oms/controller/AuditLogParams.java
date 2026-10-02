package com.oms.controller;

import com.oms.model.AuditLogFilter;
import com.oms.service.AuditLogService;
import com.oms.util.DateTimeUtil;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

// Tham số lọc của trang Nhật ký thao tác (dùng chung cho xem và xuất Excel).
// Lần đầu mở trang (không có tham số from/to) thì lọc 30 ngày gần nhất; người dùng xoá trống ngày thì không giới hạn.
record AuditLogParams(LocalDate from, LocalDate to, Long actorUserId, String entityType, String actionGroup) {

    static AuditLogParams read(HttpServletRequest request) {
        LocalDate from;
        LocalDate to;
        if (request.getParameter("from") == null && request.getParameter("to") == null) {
            to = DateTimeUtil.today();
            from = to.minusDays(AuditLogService.DEFAULT_DAYS);
        } else {
            from = parseDate(request.getParameter("from"));
            to = parseDate(request.getParameter("to"));
            if (from != null && to != null && from.isAfter(to)) {
                LocalDate swap = from;
                from = to;
                to = swap;
            }
        }
        return new AuditLogParams(from, to, AccountFormParser.parseId(request.getParameter("actorId")),
                blankToNull(request.getParameter("entityType")), blankToNull(request.getParameter("actionGroup")));
    }

    AuditLogFilter toFilter() {
        return AuditLogService.filter(from, to, actorUserId, entityType, actionGroup);
    }

    // Ô input type="date" gửi dạng yyyy-MM-dd
    private static LocalDate parseDate(String value) {
        try {
            return value == null || value.isBlank() ? null : LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
