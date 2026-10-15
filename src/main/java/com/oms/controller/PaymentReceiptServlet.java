package com.oms.controller;

import com.oms.dao.PaymentDao;
import com.oms.service.InvoiceService;
import com.oms.service.PaymentService;
import com.oms.util.FileStorage;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;

// S7-05: ảnh biên lai của khoản thu (?id=<id phiếu thu>); chỉ người thu hoặc người xem được mọi công nợ mới tải được
@WebServlet("/payments/receipt")
public class PaymentReceiptServlet extends HttpServlet {

    private final PaymentService paymentService = new PaymentService();
    private final InvoiceService invoiceService = new InvoiceService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        long userId = CurrentUser.get(request).getId();
        try {
            PaymentDao.Receipt receipt = id == null ? null : paymentService.findReceipt(id);
            if (receipt == null || (receipt.collectedBy() != userId && !invoiceService.canView(userId))) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            Path file = FileStorage.resolve(receipt.storageKey());
            if (!Files.isRegularFile(file)) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            response.setCharacterEncoding(null);
            response.setContentType(receipt.contentType());
            response.setContentLengthLong(Files.size(file));
            response.setHeader("Cache-Control", "private, max-age=86400");
            response.setHeader("X-Content-Type-Options", "nosniff");
            Files.copy(file, response.getOutputStream());
        } catch (SQLException e) {
            log("Không tải được ảnh biên lai", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
