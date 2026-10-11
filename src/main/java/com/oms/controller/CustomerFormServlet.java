package com.oms.controller;

import com.oms.dao.PriceListDao;
import com.oms.dao.RegionDao;
import com.oms.dao.WarehouseDao;
import com.oms.model.CustomerForm;
import com.oms.model.CustomerProfile;
import com.oms.model.SessionUser;
import com.oms.service.CustomerAssignmentService;
import com.oms.service.CustomerProfileService;
import com.oms.service.CustomerService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

// S3-03: form thêm (/customers/new) và sửa (/customers/edit?id=) hồ sơ đại lý. Ô Người phụ trách chỉ người có
// quyền phân công (S3-06) chọn; Nhân viên kinh doanh tạo đại lý thì tự là người phụ trách.
@WebServlet({"/customers/new", "/customers/edit"})
public class CustomerFormServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/customers/customer-form.jsp";

    private final CustomerProfileService profileService = new CustomerProfileService();
    private final CustomerService customerService = new CustomerService();
    private final CustomerAssignmentService assignmentService = new CustomerAssignmentService();
    private final PriceListDao priceListDao = new PriceListDao();
    private final RegionDao regionDao = new RegionDao();
    private final WarehouseDao warehouseDao = new WarehouseDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            CustomerProfile editing = null;
            if (isEdit(request)) {
                editing = findEditable(request, response);
                if (editing == null) {
                    return;
                }
            }
            CustomerForm form = editing == null ? CustomerForm.empty(profileService.suggestCode())
                    : CustomerForm.of(editing);
            show(request, response, form, editing, Map.of());
        } catch (SQLException e) {
            log("Không mở được form hồ sơ đại lý", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            CustomerProfile editing = null;
            if (isEdit(request)) {
                editing = findEditable(request, response);
                if (editing == null) {
                    return;
                }
            }
            SessionUser user = CurrentUser.get(request);
            CustomerForm form = parse(request, editing);
            boolean canAssign = CustomerAssignmentServlet.canAssign(request);
            // Nhân viên kinh doanh (chỉ xem đại lý mình) tạo đại lý thì tự phụ trách để vẫn thấy được đại lý vừa tạo
            Long selfAssignId = !canAssign && !customerService.canViewAll(user.getId()) ? user.getId() : null;
            CustomerProfileService.Checked checked = profileService.validate(form, editing, canAssign, selfAssignId,
                    options());
            if (!checked.isValid()) {
                show(request, response, form, editing, checked.errors());
                return;
            }
            long id;
            String message;
            if (editing == null) {
                id = profileService.create(checked, user.getId(), request.getRemoteAddr());
                message = "Đã thêm đại lý " + checked.values().code() + " - " + checked.values().name() + ".";
            } else {
                long version = parseVersion(form.getVersion());
                if (!profileService.update(editing, version, checked, user.getId(), request.getRemoteAddr())) {
                    show(request, response, form, profileService.find(editing.getId()), Map.of("form",
                            "Hồ sơ vừa được người khác cập nhật. Mở lại hồ sơ để xem bản mới nhất trước khi sửa."));
                    return;
                }
                id = editing.getId();
                message = "Đã cập nhật hồ sơ đại lý " + editing.getCode() + ".";
            }
            request.getSession().setAttribute(CustomerProfileServlet.FLASH_MESSAGE, message);
            response.sendRedirect(request.getContextPath() + "/customers/profile?id=" + id);
        } catch (SQLException e) {
            log("Không lưu được hồ sơ đại lý", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static boolean isEdit(HttpServletRequest request) {
        return "/customers/edit".equals(request.getServletPath());
    }

    // null (đã trả 404) nếu không có đại lý hoặc người dùng không sửa được đại lý này
    private CustomerProfile findEditable(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        Long id = AccountFormParser.parseId(request.getParameter("id"));
        CustomerProfile profile = id == null || !CustomerProfileServlet.canManage(CurrentUser.get(request), id)
                ? null : profileService.find(id);
        if (profile == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
        return profile;
    }

    private CustomerProfileService.Options options() throws SQLException {
        return new CustomerProfileService.Options(priceListDao.findActiveGroups(), regionDao.findActive(),
                warehouseDao.findActive(), assignmentService.getActiveSalesReps());
    }

    private void show(HttpServletRequest request, HttpServletResponse response, CustomerForm form,
                      CustomerProfile editing, Map<String, String> errors)
            throws ServletException, IOException, SQLException {
        CustomerProfileService.Options options = options();
        request.setAttribute("form", form);
        request.setAttribute("editing", editing);
        request.setAttribute("errors", errors);
        request.setAttribute("groups", options.groups());
        request.setAttribute("regions", options.regions());
        request.setAttribute("warehouses", options.warehouses());
        request.setAttribute("activeReps", options.activeReps());
        request.setAttribute("canAssign", CustomerAssignmentServlet.canAssign(request));
        request.getRequestDispatcher(VIEW).forward(request, response);
    }

    private static CustomerForm parse(HttpServletRequest request, CustomerProfile editing) {
        String phone = normalize(request.getParameter("phone"));
        // Cho gõ "0903 123 456" hay "0903.123.456"; lưu dạng liền chữ số
        phone = phone == null ? null : phone.replaceAll("[\\s.-]", "");
        return new CustomerForm(editing == null ? null : editing.getId(), normalize(request.getParameter("code")),
                normalize(request.getParameter("name")), normalize(request.getParameter("taxCode")), phone,
                normalize(request.getParameter("email")), normalize(request.getParameter("address")),
                normalize(request.getParameter("customerGroupId")), normalize(request.getParameter("regionId")),
                normalize(request.getParameter("salesRepId")), normalize(request.getParameter("defaultWarehouseId")),
                normalize(request.getParameter("status")), normalize(request.getParameter("version")));
    }

    private static long parseVersion(String value) {
        try {
            return value == null ? -1 : Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
