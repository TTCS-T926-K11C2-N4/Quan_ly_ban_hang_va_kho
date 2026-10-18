package com.oms.controller;

import com.oms.model.DeliveryPoint;
import com.oms.model.DeliveryStop;
import com.oms.service.DeliveryConfirmService;
import com.oms.util.DateTimeUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// S6-08: ghi nhận kết quả giao cho các đơn được chọn ở một điểm giao (?result=success|fail&address=&date=&note=...).
// Giao thành công: người nhận, thời điểm giao, ảnh biên bản có chữ ký (ô file "proofs", JPG/PNG, tối đa 5 ảnh x 5MB).
// Giao không thành công: chọn lý do (Lý do khác thì ghi rõ), hàng quay về kho.
@WebServlet("/deliveries/confirm")
@MultipartConfig(maxFileSize = DeliveryConfirmService.MAX_PROOF_BYTES,
        maxRequestSize = DeliveryConfirmService.MAX_PROOF_BYTES * DeliveryConfirmService.MAX_PROOFS + 1024 * 1024)
public class DeliveryConfirmServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/deliveries/delivery-confirm.jsp";
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final DeliveryConfirmService deliveryService = new DeliveryConfirmService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            Context context = load(request, response);
            if (context == null) {
                return;
            }
            Map<String, String> errors = Map.of();
            if (context.success()) {
                forward(request, response, context, new DeliveryConfirmService.SuccessForm(
                        context.point().getReceiverName(), nowLocal().toString(), null), null, errors);
            } else {
                forward(request, response, context, null, new DeliveryConfirmService.FailureForm(null, null), errors);
            }
        } catch (SQLException e) {
            log("Không tải được form xác nhận giao", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long actorUserId = CurrentUser.get(request).getId();
        try {
            Context context;
            List<DeliveryConfirmService.ProofUpload> proofs = new ArrayList<>();
            String uploadError = null;
            try {
                context = load(request, response);
                if (context == null) {
                    return;
                }
                if (context.success()) {
                    uploadError = readProofs(request, proofs);
                }
            } catch (IllegalStateException e) {
                // Tomcat báo vượt maxFileSize / maxRequestSize bằng IllegalStateException
                request.getSession().setAttribute(DeliveryListServlet.FLASH_ERROR,
                        "Ảnh vượt dung lượng cho phép (tối đa 5MB mỗi ảnh, 5 ảnh). Chọn lại ảnh nhỏ hơn.");
                response.sendRedirect(request.getContextPath() + "/deliveries");
                return;
            }
            List<Long> noteIds = context.stops().stream().map(DeliveryStop::getNoteId).toList();
            int count;
            String message;
            if (context.success()) {
                DeliveryConfirmService.SuccessForm form = new DeliveryConfirmService.SuccessForm(
                        trim(request.getParameter("recipientName")), trim(request.getParameter("deliveredAt")),
                        trim(request.getParameter("remark")));
                Map<String, String> errors = new LinkedHashMap<>(
                        DeliveryConfirmService.validateSuccess(form, proofs.size(), nowLocal()));
                if (uploadError != null) {
                    errors.put("proofs", uploadError);
                }
                if (!errors.isEmpty()) {
                    forward(request, response, context, form, null, errors);
                    return;
                }
                count = deliveryService.confirmSuccess(noteIds, form, proofs, actorUserId, request.getRemoteAddr());
                message = "Đã xác nhận giao thành công " + count + " đơn tại " + context.point().getCustomerName()
                        + ". Phiếu đã giao đủ điều kiện lập hoá đơn.";
            } else {
                DeliveryConfirmService.FailureForm form = new DeliveryConfirmService.FailureForm(
                        trim(request.getParameter("reason")), trim(request.getParameter("remark")));
                Map<String, String> errors = DeliveryConfirmService.validateFailure(form);
                if (!errors.isEmpty()) {
                    forward(request, response, context, null, form, errors);
                    return;
                }
                count = deliveryService.confirmFailure(noteIds, form, actorUserId, request.getRemoteAddr());
                message = "Đã ghi nhận giao không thành công " + count + " đơn; hàng đã quay về kho.";
            }
            request.getSession().setAttribute(DeliveryListServlet.FLASH_MESSAGE, message);
            response.sendRedirect(pointUrl(request, context));
        } catch (DeliveryConfirmService.DeliveryException e) {
            request.getSession().setAttribute(DeliveryListServlet.FLASH_ERROR, e.getMessage());
            response.sendRedirect(request.getContextPath() + "/deliveries/point?address="
                    + AccountFormParser.parseId(request.getParameter("address")) + "&date="
                    + DeliveryListServlet.parseDate(request.getParameter("date")));
        } catch (SQLException e) {
            log("Không ghi nhận được giao hàng", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private record Context(DeliveryPoint point, LocalDate date, boolean success, List<DeliveryStop> stops) {
    }

    // null nếu đã trả lỗi / chuyển hướng
    private Context load(HttpServletRequest request, HttpServletResponse response) throws SQLException, IOException {
        Long addressId = AccountFormParser.parseId(request.getParameter("address"));
        LocalDate date = DeliveryListServlet.parseDate(request.getParameter("date"));
        String result = request.getParameter("result");
        long userId = CurrentUser.get(request).getId();
        DeliveryPoint point = addressId == null ? null
                : deliveryService.point(addressId, date, deliveryService.scope(userId));
        if (point == null || !("success".equals(result) || "fail".equals(result))) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        List<Long> noteIds = new ArrayList<>();
        String[] values = request.getParameterValues("note");
        if (values != null) {
            for (String value : values) {
                Long id = AccountFormParser.parseId(value);
                if (id != null) {
                    noteIds.add(id);
                }
            }
        }
        List<DeliveryStop> stops = DeliveryConfirmService.selectStops(point, noteIds);
        Context context = new Context(point, date, "success".equals(result), stops);
        String error = null;
        if (stops.isEmpty()) {
            error = "Điểm giao không còn đơn chờ giao trong phạm vi đã chọn.";
        } else {
            for (DeliveryStop stop : stops) {
                if (!deliveryService.canUseWarehouse(userId, stop.getWarehouseId())) {
                    error = "Bạn chỉ xác nhận giao hàng cho phiếu xuất của kho mình phụ trách.";
                    break;
                }
            }
        }
        if (error != null) {
            request.getSession().setAttribute(DeliveryListServlet.FLASH_ERROR, error);
            response.sendRedirect(pointUrl(request, context));
            return null;
        }
        return context;
    }

    // Đọc các ảnh đã chọn; trả về lỗi nếu có ảnh không phải JPG/PNG
    private static String readProofs(HttpServletRequest request, List<DeliveryConfirmService.ProofUpload> proofs)
            throws IOException, ServletException {
        for (Part part : request.getParts()) {
            if (!"proofs".equals(part.getName()) || part.getSize() == 0) {
                continue;
            }
            byte[] content;
            try (InputStream in = part.getInputStream()) {
                content = in.readAllBytes();
            }
            String name = fileName(part.getSubmittedFileName());
            if (DeliveryConfirmService.detectImageType(content) == null) {
                return "Tệp \"" + name + "\" không phải ảnh JPG hoặc PNG.";
            }
            proofs.add(new DeliveryConfirmService.ProofUpload(content, name));
        }
        return null;
    }

    private static void forward(HttpServletRequest request, HttpServletResponse response, Context context,
                                DeliveryConfirmService.SuccessForm successForm,
                                DeliveryConfirmService.FailureForm failureForm, Map<String, String> errors)
            throws ServletException, IOException {
        request.setAttribute("point", context.point());
        request.setAttribute("date", context.date());
        request.setAttribute("dateText", DateTimeUtil.formatDay(context.date()));
        request.setAttribute("success", context.success());
        request.setAttribute("stops", context.stops());
        request.setAttribute("successForm", successForm);
        request.setAttribute("failureForm", failureForm);
        request.setAttribute("errors", errors);
        request.setAttribute("failureReasons", DeliveryConfirmService.FAILURE_REASONS);
        request.setAttribute("otherReason", DeliveryConfirmService.OTHER_REASON);
        request.setAttribute("maxProofs", DeliveryConfirmService.MAX_PROOFS);
        request.setAttribute("nowLocal", nowLocal().toString());
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static String pointUrl(HttpServletRequest request, Context context) {
        return request.getContextPath() + "/deliveries/point?address=" + context.point().getAddressId()
                + "&date=" + context.date();
    }

    // Giờ Việt Nam tới phút, dạng của ô datetime-local
    private static LocalDateTime nowLocal() {
        return LocalDateTime.now(DISPLAY_ZONE).truncatedTo(ChronoUnit.MINUTES);
    }

    private static String fileName(String submitted) {
        if (submitted == null || submitted.isBlank()) {
            return "bien-ban.jpg";
        }
        String name = Paths.get(submitted.replace('\\', '/')).getFileName().toString();
        return name.length() > 255 ? name.substring(name.length() - 255) : name;
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
