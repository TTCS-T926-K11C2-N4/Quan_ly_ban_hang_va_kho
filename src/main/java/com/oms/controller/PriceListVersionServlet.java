package com.oms.controller;

import com.oms.model.PriceList;
import com.oms.model.PriceListForm;
import com.oms.service.PriceListService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Map;

// S2-10: tạo phiên bản mới từ một bảng giá (chép dòng giá để sửa); bản cũ kết thúc hôm trước ngày bản mới bắt đầu.
// Phiên bản giữ nguyên nhóm khách hàng của bản cũ.
@WebServlet("/price-lists/version")
public class PriceListVersionServlet extends HttpServlet {

    private final PriceListService priceListService = new PriceListService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            PriceList previous = find(request);
            if (previous == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (redirectIfHasNext(request, response, previous)) {
                return;
            }
            String start = PriceListService.earliestVersionStart(previous).toString();
            String end = previous.getValidTo() == null || !previous.getValidTo().isAfter(LocalDate.parse(start))
                    ? null : previous.getValidTo().toString();
            PriceListForm form = priceListService.toForm(previous, start, end);
            PriceListCreateServlet.showForm(request, response, "version", previous, form, Map.of(), priceListService);
        } catch (SQLException e) {
            log("Không tải được bảng giá", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            PriceList previous = find(request);
            if (previous == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            if (redirectIfHasNext(request, response, previous)) {
                return;
            }
            PriceListForm read = PriceListFormParser.read(request);
            PriceListForm form = new PriceListForm(previous.getCustomerGroupId(), read.getName(), read.getValidFrom(),
                    read.getValidTo(), read.getLines());
            Map<String, String> errors = priceListService.validate(form, null, previous);
            if (!errors.isEmpty()) {
                PriceListCreateServlet.showForm(request, response, "version", previous, form, errors,
                        priceListService);
                return;
            }
            long id = priceListService.createVersion(previous, form, CurrentUser.get(request).getId(),
                    request.getRemoteAddr());
            request.getSession().setAttribute(PriceListListServlet.FLASH_MESSAGE, "Đã tạo phiên bản mới từ bảng giá "
                    + previous.getCode() + ". Bản cũ kết thúc hiệu lực trước ngày bản mới bắt đầu.");
            response.sendRedirect(request.getContextPath() + "/price-lists?id=" + id);
        } catch (SQLException e) {
            log("Không tạo được phiên bản bảng giá", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private PriceList find(HttpServletRequest request) throws SQLException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        return id == null ? null : priceListService.find(id);
    }

    // Chỉ tạo phiên bản từ bản mới nhất, để chuỗi phiên bản không bị rẽ nhánh
    private boolean redirectIfHasNext(HttpServletRequest request, HttpServletResponse response, PriceList previous)
            throws IOException, SQLException {
        if (!priceListService.hasNextVersion(previous.getId())) {
            return false;
        }
        request.getSession().setAttribute(PriceListListServlet.FLASH_ERROR, "Bảng giá " + previous.getCode()
                + " đã có phiên bản sau. Hãy tạo phiên bản từ bản mới nhất.");
        response.sendRedirect(request.getContextPath() + "/price-lists?id=" + previous.getId());
        return true;
    }
}
