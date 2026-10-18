package com.oms.controller;

import com.oms.service.InvoiceService;
import com.oms.service.PaymentReminderService;
import com.oms.util.DateTimeUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;

// S7-08: kế toán chạy ngay tác vụ nhắc hạn (ngoài lịch hằng ngày); không gửi trùng nhờ payment_reminder_logs
@WebServlet("/invoices/reminders")
public class ReminderRunServlet extends HttpServlet {

    private final PaymentReminderService reminderService = new PaymentReminderService();
    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            if (!invoiceService.canView(CurrentUser.get(request).getId())) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            int sent = reminderService.run(DateTimeUtil.today());
            request.getSession().setAttribute(InvoiceListServlet.FLASH_MESSAGE, sent == 0
                    ? "Không có hoá đơn nào cần nhắc hạn hôm nay (hoặc đã nhắc rồi)."
                    : "Đã gửi " + sent + " nhắc hạn thanh toán.");
            response.sendRedirect(request.getContextPath() + "/invoices");
        } catch (SQLException e) {
            log("Không gửi được nhắc hạn thanh toán", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
