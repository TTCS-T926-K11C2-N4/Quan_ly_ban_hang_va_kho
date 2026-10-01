package com.oms.controller;

import com.oms.model.ImportBatch;
import com.oms.model.ImportRow;
import com.oms.model.PageResult;
import com.oms.service.AccountImportService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

// S2-01 Bước 2: xem trước, lọc các dòng đã đọc (GET) và xác nhận nhập các dòng hợp lệ (POST)
@WebServlet("/accounts/import/preview")
public class AccountImportPreviewServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/accounts/import-preview.jsp";
    private static final int PAGE_SIZE = 20;

    private final AccountImportService importService = new AccountImportService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        ImportBatch batch = session == null ? null : (ImportBatch) session.getAttribute(AccountImportServlet.SESSION_BATCH);
        if (batch == null) {
            response.sendRedirect(request.getContextPath() + "/accounts/import");
            return;
        }

        String keyword = request.getParameter("keyword");
        keyword = keyword == null ? "" : keyword.trim();
        String status = request.getParameter("status");
        if (!"valid".equals(status) && !"update".equals(status) && !"error".equals(status)) {
            status = "all";
        }
        List<ImportRow> filtered = batch.getRows().stream()
                .filter(statusFilter(status))
                .filter(keywordFilter(keyword))
                .toList();

        int totalPages = Math.max(1, (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(Math.max(1, parsePage(request.getParameter("page"))), totalPages);
        int from = (page - 1) * PAGE_SIZE;
        List<ImportRow> items = filtered.subList(from, Math.min(from + PAGE_SIZE, filtered.size()));

        request.setAttribute("batch", batch);
        request.setAttribute("rowPage", new PageResult<>(items, page, PAGE_SIZE, filtered.size()));
        request.setAttribute("keyword", keyword);
        request.setAttribute("statusFilter", status);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        ImportBatch batch = session == null ? null : (ImportBatch) session.getAttribute(AccountImportServlet.SESSION_BATCH);
        if (batch == null) {
            // Đã nhập rồi (vd bấm nút hai lần, hoặc bấm F5) thì không nhập lại
            response.sendRedirect(request.getContextPath() + "/accounts/import");
            return;
        }
        session.removeAttribute(AccountImportServlet.SESSION_BATCH);
        try {
            importService.importValidRows(batch, CurrentUser.get(request).getId(), request.getRemoteAddr(),
                    AppUrl.of(request, "/login"));
        } catch (SQLException e) {
            log("Không nhập được người dùng từ Excel", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        session.setAttribute(AccountImportServlet.SESSION_RESULT, batch);
        response.sendRedirect(request.getContextPath() + "/accounts/import/result");
    }

    private static Predicate<ImportRow> statusFilter(String status) {
        return switch (status) {
            case "valid" -> ImportRow::isValid;
            case "error" -> row -> !row.isValid();
            // Nhập người dùng không cập nhật tài khoản có sẵn
            case "update" -> row -> false;
            default -> row -> true;
        };
    }

    private static Predicate<ImportRow> keywordFilter(String keyword) {
        if (keyword.isEmpty()) {
            return row -> true;
        }
        String needle = keyword.toLowerCase(Locale.ROOT);
        return row -> contains(row.getFullName(), needle) || contains(row.getEmail(), needle)
                || contains(row.getPhone(), needle.replaceAll("\\s", "")) || contains(row.getUsername(), needle);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static int parsePage(String value) {
        try {
            return value == null ? 1 : Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 1;
        }
    }
}
