package com.oms.controller;

import com.oms.dao.PaymentDao;
import com.oms.service.InvoiceService;
import com.oms.service.PaymentService;
import com.oms.util.DateTimeUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

// S7-05: bảng đối chiếu tiền thu theo từng nhân viên và từng ngày (mặc định 30 ngày gần nhất)
@WebServlet("/collections/reconcile")
public class ReconciliationServlet extends HttpServlet {

    private static final int DEFAULT_DAYS = 30;

    private final PaymentService paymentService = new PaymentService();
    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        LocalDate today = DateTimeUtil.today();
        LocalDate to = parseDate(request.getParameter("to"), today);
        LocalDate from = parseDate(request.getParameter("from"), to.minusDays(DEFAULT_DAYS));
        if (from.isAfter(to)) {
            from = to;
        }
        try {
            if (!invoiceService.canView(CurrentUser.get(request).getId())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            List<PaymentDao.ReconciliationRow> rows = paymentService.reconciliation(from, to);
            BigDecimal total = BigDecimal.ZERO;
            BigDecimal pending = BigDecimal.ZERO;
            BigDecimal confirmed = BigDecimal.ZERO;
            BigDecimal voided = BigDecimal.ZERO;
            for (PaymentDao.ReconciliationRow row : rows) {
                total = total.add(row.getTotal());
                pending = pending.add(row.getPending());
                confirmed = confirmed.add(row.getConfirmed());
                voided = voided.add(row.getVoided());
            }
            request.setAttribute("rows", rows);
            request.setAttribute("sumTotal", total);
            request.setAttribute("sumPending", pending);
            request.setAttribute("sumConfirmed", confirmed);
            request.setAttribute("sumVoided", voided);
        } catch (SQLException e) {
            log("Không tải được bảng đối chiếu tiền thu", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }
        request.setAttribute("from", from.toString());
        request.setAttribute("to", to.toString());
        request.getRequestDispatcher("/WEB-INF/views/payments/reconciliation.jsp").forward(request, response);
    }

    private static LocalDate parseDate(String value, LocalDate fallback) {
        try {
            return value == null || value.isBlank() ? fallback : LocalDate.parse(value.trim());
        } catch (DateTimeParseException e) {
            return fallback;
        }
    }
}
