package com.oms.controller;

import com.oms.service.PaymentService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;

// S7-05: ghi nhận khoản thu tại cửa hàng kèm ảnh biên lai; khoản thu ở trạng thái Chờ nộp
@WebServlet("/collections/new")
@MultipartConfig(maxFileSize = PaymentService.RECEIPT_MAX_BYTES,
        maxRequestSize = PaymentService.RECEIPT_MAX_BYTES + 64 * 1024)
public class CollectionNewServlet extends HttpServlet {

    private final PaymentService paymentService = new PaymentService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(request.getContextPath() + "/collections");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Long customerId = AccountFormParser.parseId(request.getParameter("customer"));
        PaymentService.ReceiptUpload receipt = null;
        try {
            Part part = request.getPart("receipt");
            if (part != null && part.getSize() > 0) {
                try (InputStream in = part.getInputStream()) {
                    receipt = new PaymentService.ReceiptUpload(in.readAllBytes(), part.getSubmittedFileName());
                }
            }
        } catch (IllegalStateException e) {
            // Vượt dung lượng tối đa của @MultipartConfig
            fail(request, response, "Ảnh biên lai tối đa " + PaymentService.RECEIPT_MAX_BYTES / (1024 * 1024)
                    + "MB.");
            return;
        }
        try {
            if (customerId == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            PaymentService.Outcome outcome = paymentService.collect(CurrentUser.get(request).getId(), customerId,
                    request.getParameter("amount"), request.getParameter("method"), request.getParameter("date"),
                    request.getParameter("note"), receipt, request.getRemoteAddr());
            if (outcome.isNotFound()) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (outcome.getError() != null) {
                fail(request, response, outcome.getError());
                return;
            }
            request.getSession().setAttribute(CollectionServlet.FLASH_MESSAGE, "Đã ghi nhận khoản thu "
                    + outcome.getReceiptNo() + ". Khoản thu ở trạng thái Chờ nộp cho tới khi kế toán xác nhận.");
            response.sendRedirect(request.getContextPath() + "/collections");
        } catch (SQLException e) {
            log("Không ghi nhận được khoản thu theo tuyến", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void fail(HttpServletRequest request, HttpServletResponse response, String error)
            throws ServletException, IOException {
        request.setAttribute("formError", error);
        request.setAttribute("customer", request.getParameter("customer"));
        request.setAttribute("amount", request.getParameter("amount"));
        request.setAttribute("method", request.getParameter("method"));
        request.setAttribute("date", request.getParameter("date"));
        request.setAttribute("note", request.getParameter("note"));
        try {
            CollectionServlet.show(request, response);
        } catch (SQLException e) {
            log("Không tải được khoản thu theo tuyến", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
