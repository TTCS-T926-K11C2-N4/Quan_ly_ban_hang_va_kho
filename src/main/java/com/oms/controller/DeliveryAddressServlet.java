package com.oms.controller;

import com.oms.model.Customer;
import com.oms.model.DeliveryAddressEntry;
import com.oms.model.DeliveryAddressForm;
import com.oms.model.Permission;
import com.oms.service.CustomerService;
import com.oms.service.DeliveryAddressService;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

// S3-04: tab Điểm giao hàng trong chi tiết đại lý (?id=). Người sửa được đại lý (CUSTOMER_MANAGE trong phạm vi
// của mình) thấy nút thêm, sửa, xoá, đặt mặc định; ?edit=idĐiểm mở thẻ đó thành form, ?edit=new mở thẻ thêm mới.
@WebServlet("/customers/delivery-addresses")
public class DeliveryAddressServlet extends HttpServlet {

    static final String VIEW = "/WEB-INF/views/customers/delivery-addresses.jsp";
    static final String FLASH_MESSAGE = "deliveryAddressFlashMessage";
    static final String NEW = "new";

    private static final CustomerService CUSTOMER_SERVICE = new CustomerService();
    private static final DeliveryAddressService ADDRESS_SERVICE = new DeliveryAddressService();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String idParam = request.getParameter("id");
        if (idParam == null) {
            response.sendRedirect(request.getContextPath() + "/customers");
            return;
        }
        Long id = AccountFormParser.parseId(idParam);
        long userId = CurrentUser.get(request).getId();
        try {
            Customer customer = id == null ? null : CUSTOMER_SERVICE.findVisible(userId, Permission.CUSTOMER_VIEW, id);
            if (customer == null) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            boolean canManage = canManage(request, customer);
            String edit = request.getParameter("edit");
            DeliveryAddressForm form = null;
            if (canManage && NEW.equals(edit)) {
                form = DeliveryAddressForm.empty();
            } else if (canManage && edit != null) {
                Long addressId = AccountFormParser.parseId(edit);
                DeliveryAddressEntry entry = addressId == null ? null
                        : ADDRESS_SERVICE.findEntry(customer.getId(), addressId);
                if (entry == null || !entry.isActive()) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                form = DeliveryAddressForm.of(entry);
            }
            Flash.moveToRequest(request, FLASH_MESSAGE, "flashMessage");
            show(request, response, customer, canManage, form, Map.of());
        } catch (SQLException e) {
            log("Không tải được điểm giao hàng của đại lý", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    // Nhân viên kinh doanh chỉ sửa được điểm giao của đại lý mình phụ trách (data_scope ASSIGNED)
    static boolean canManage(HttpServletRequest request, Customer customer) throws SQLException {
        return CurrentUser.get(request).can(Permission.CUSTOMER_MANAGE) && CUSTOMER_SERVICE.findVisible(
                CurrentUser.get(request).getId(), Permission.CUSTOMER_MANAGE, customer.getId()) != null;
    }

    // form = null: không mở thẻ nào để sửa; form.id = null: thẻ thêm điểm giao mới
    static void show(HttpServletRequest request, HttpServletResponse response, Customer customer, boolean canManage,
                     DeliveryAddressForm form, Map<String, String> errors)
            throws ServletException, IOException, SQLException {
        request.setAttribute("customer", customer);
        request.setAttribute("addresses", ADDRESS_SERVICE.findEntries(customer.getId()));
        request.setAttribute("canManage", canManage);
        request.setAttribute("form", form);
        request.setAttribute("errors", errors);
        request.getRequestDispatcher(VIEW).forward(request, response);
    }
}
