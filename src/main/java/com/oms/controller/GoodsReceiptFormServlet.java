package com.oms.controller;

import com.oms.model.GoodsReceiptDetail;
import com.oms.model.GoodsReceiptForm;
import com.oms.model.ReceiptProduct;
import com.oms.model.SelectOption;
import com.oms.service.GoodsReceiptService;
import com.oms.util.DateTimeUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// S5-04: form Tạo phiếu nhập kho (/goods-receipts/new, ?adjusts=id để lập phiếu điều chỉnh cho phiếu đã ghi sổ)
// và Sửa phiếu nháp (/goods-receipts/edit?id=). Nút "Lưu nháp" chỉ lưu; "Ghi sổ" lưu rồi cộng tồn luôn.
@WebServlet({"/goods-receipts/new", "/goods-receipts/edit"})
public class GoodsReceiptFormServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/goods-receipts/receipt-form.jsp";

    private final GoodsReceiptService receiptService = new GoodsReceiptService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            GoodsReceiptService.Options options = receiptService.getOptions(CurrentUser.get(request).getId());
            if (isEdit(request)) {
                GoodsReceiptDetail editing = findReceipt(request.getParameter("id"));
                if (editing == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                } else if (!editing.isDraft()) {
                    // Phiếu đã ghi sổ không sửa được, chỉ xem
                    response.sendRedirect(request.getContextPath() + "/goods-receipts/view?id=" + editing.getId());
                } else if (!receiptService.canUseWarehouse(options, editing.getWarehouseId())) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                } else {
                    show(request, response, GoodsReceiptFormParser.of(editing), options, editing,
                            findReceipt(editing.getAdjustsReceiptId() == null ? null
                                    : editing.getAdjustsReceiptId().toString()), Map.of());
                }
                return;
            }
            GoodsReceiptDetail original = null;
            if (request.getParameter("adjusts") != null) {
                original = findReceipt(request.getParameter("adjusts"));
                if (original == null || !original.isConfirmed()) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                if (!receiptService.canUseWarehouse(options, original.getWarehouseId())) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN);
                    return;
                }
            }
            show(request, response, emptyForm(options, original), options, null, original, Map.of());
        } catch (SQLException e) {
            log("Không tải được form phiếu nhập", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        long actorId = CurrentUser.get(request).getId();
        try {
            GoodsReceiptService.Options options = receiptService.getOptions(actorId);
            GoodsReceiptDetail editing = null;
            String adjustsId = request.getParameter("adjustsReceiptId");
            adjustsId = adjustsId == null || adjustsId.isBlank() ? null : adjustsId.trim();
            if (isEdit(request)) {
                editing = findReceipt(request.getParameter("id"));
                if (editing == null) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                if (!editing.isDraft()) {
                    request.getSession().setAttribute(GoodsReceiptListServlet.FLASH_MESSAGE,
                            "Phiếu " + editing.getReceiptNo() + " đã được ghi sổ nên không sửa được nữa.");
                    response.sendRedirect(request.getContextPath() + "/goods-receipts/view?id=" + editing.getId());
                    return;
                }
                adjustsId = editing.getAdjustsReceiptId() == null ? null : editing.getAdjustsReceiptId().toString();
            }
            GoodsReceiptDetail original = findReceipt(adjustsId);
            if (adjustsId != null && (original == null || !original.isConfirmed())) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST);
                return;
            }
            GoodsReceiptForm form = GoodsReceiptFormParser.parse(request,
                    editing == null ? null : Long.toString(editing.getId()), adjustsId);
            boolean post = "post".equals(request.getParameter("action"));
            Map<String, String> errors = receiptService.validate(form, options, editing, original);
            GoodsReceiptService.Saved saved = null;
            if (errors.isEmpty()) {
                try {
                    saved = receiptService.save(form, options, editing, post, actorId, request.getRemoteAddr());
                } catch (GoodsReceiptService.StockShortageException e) {
                    errors = new LinkedHashMap<>(Map.of("post", e.getMessage()));
                }
            }
            if (!errors.isEmpty()) {
                show(request, response, form, options, editing, original, errors);
                return;
            }
            if (saved == null) {
                request.getSession().setAttribute(GoodsReceiptListServlet.FLASH_ERROR,
                        "Phiếu " + editing.getReceiptNo() + " vừa được ghi sổ ở nơi khác nên không lưu được thay đổi.");
                response.sendRedirect(request.getContextPath() + "/goods-receipts");
                return;
            }
            if (post) {
                request.getSession().setAttribute(GoodsReceiptListServlet.FLASH_MESSAGE,
                        "Đã ghi sổ phiếu " + saved.receiptNo() + ". Tồn kho đã được cập nhật.");
                response.sendRedirect(request.getContextPath() + "/goods-receipts/view?id=" + saved.id());
            } else {
                request.getSession().setAttribute(GoodsReceiptListServlet.FLASH_MESSAGE,
                        "Đã lưu nháp phiếu " + saved.receiptNo() + ". Phiếu nháp chưa cộng tồn.");
                response.sendRedirect(request.getContextPath() + "/goods-receipts");
            }
        } catch (SQLException e) {
            log("Không lưu được phiếu nhập", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static boolean isEdit(HttpServletRequest request) {
        return "/goods-receipts/edit".equals(request.getServletPath());
    }

    private GoodsReceiptDetail findReceipt(String id) throws SQLException {
        Long parsed = AccountFormParser.parseId(id);
        return parsed == null ? null : receiptService.find(parsed);
    }

    // Phiếu mới: ngày nhập hôm nay, một dòng hàng trống; chỉ một kho được chọn thì chọn sẵn.
    // Phiếu điều chỉnh lấy nhà cung cấp, kho của phiếu gốc.
    private static GoodsReceiptForm emptyForm(GoodsReceiptService.Options options, GoodsReceiptDetail original) {
        String supplierId = original == null ? null : Long.toString(original.getSupplierId());
        String warehouseId = original != null ? Long.toString(original.getWarehouseId())
                : options.warehouses().size() == 1 ? Long.toString(options.warehouses().get(0).getId()) : null;
        return new GoodsReceiptForm(null, original == null ? null : Long.toString(original.getId()), supplierId,
                warehouseId, DateTimeUtil.today().toString(), null, null,
                List.of(new GoodsReceiptForm.Line(null, null, null, null, null, null, null, null)));
    }

    private void show(HttpServletRequest request, HttpServletResponse response, GoodsReceiptForm form,
                      GoodsReceiptService.Options options, GoodsReceiptDetail editing, GoodsReceiptDetail original,
                      Map<String, String> errors) throws ServletException, IOException, SQLException {
        // Khoá chuỗi để JSP tra theo chuỗi id trong form
        Map<String, ReceiptProduct> productsById = new LinkedHashMap<>();
        options.products().forEach(product -> productsById.put(Long.toString(product.getId()), product));
        Map<String, List<SelectOption>> locationsByWarehouse = new LinkedHashMap<>();
        options.locations().forEach((warehouseId, locations) -> locationsByWarehouse.put(warehouseId.toString(), locations));
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.setAttribute("options", options);
        request.setAttribute("productsById", productsById);
        request.setAttribute("locationsByWarehouse", locationsByWarehouse);
        request.setAttribute("editing", editing);
        request.setAttribute("original", original);
        if (editing != null && options.suppliers().stream().noneMatch(s -> s.getId() == editing.getSupplierId())) {
            request.setAttribute("currentSupplier", new SelectOption(editing.getSupplierId(), editing.getSupplierName()));
        }
        request.setAttribute("receiptNo", editing != null ? editing.getReceiptNo() : receiptService.previewNextReceiptNo());
        request.setAttribute("today", DateTimeUtil.today().toString());
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
