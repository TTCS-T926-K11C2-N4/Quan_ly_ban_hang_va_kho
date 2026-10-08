package com.oms.service;

import com.oms.dao.CustomerDao;
import com.oms.dao.PermissionDao;
import com.oms.model.Customer;
import com.oms.model.CustomerFilter;
import com.oms.model.CustomerListItem;
import com.oms.model.PageResult;
import com.oms.model.Permission;
import com.oms.model.SelectOption;

import java.sql.SQLException;
import java.util.List;

// Đại lý mà người dùng được xem theo data_scope của quyền: ALL mọi đại lý, ASSIGNED chỉ đại lý mình phụ trách
// (S3-06). OWN là tài khoản đại lý, không dùng các màn nội bộ này nên không thấy đại lý nào.
public class CustomerService {

    private static final String SCOPE_ALL = "ALL";
    private static final String SCOPE_ASSIGNED = "ASSIGNED";
    public static final int PAGE_SIZE = 10;

    private final CustomerDao customerDao = new CustomerDao();
    private final PermissionDao permissionDao = new PermissionDao();

    public List<Customer> findVisibleCustomers(long userId, String permission) throws SQLException {
        String scope = permissionDao.findScope(userId, permission);
        if (SCOPE_ALL.equals(scope)) {
            return customerDao.findAll(null);
        }
        return SCOPE_ASSIGNED.equals(scope) ? customerDao.findAll(userId) : List.of();
    }

    // S3-08: danh sách đại lý có tìm kiếm, lọc, phân trang trong phạm vi người dùng được xem
    public PageResult<CustomerListItem> search(long userId, CustomerFilter filter, int requestedPage)
            throws SQLException {
        String scope = permissionDao.findScope(userId, Permission.CUSTOMER_VIEW);
        if (!SCOPE_ALL.equals(scope) && !SCOPE_ASSIGNED.equals(scope)) {
            return new PageResult<>(List.of(), 1, PAGE_SIZE, 0);
        }
        Long scopeSalesRepId = SCOPE_ALL.equals(scope) ? null : userId;
        long total = customerDao.count(filter, scopeSalesRepId);
        int totalPages = (int) Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(Math.max(requestedPage, 1), totalPages);
        return new PageResult<>(customerDao.findPage(filter, scopeSalesRepId, (page - 1) * PAGE_SIZE, PAGE_SIZE),
                page, PAGE_SIZE, total);
    }

    // Lọc theo người phụ trách chỉ có nghĩa khi xem được đại lý của mọi người; Nhân viên kinh doanh chỉ thấy đại lý mình
    public boolean canViewAll(long userId) throws SQLException {
        return SCOPE_ALL.equals(permissionDao.findScope(userId, Permission.CUSTOMER_VIEW));
    }

    public List<SelectOption> getSalesRepOptions() throws SQLException {
        return customerDao.findSalesRepOptions();
    }

    // null nếu không có đại lý này hoặc người dùng không được xem (trả 404 như nhau để không lộ đại lý của người khác)
    public Customer findVisible(long userId, String permission, long customerId) throws SQLException {
        Customer customer = customerDao.findById(customerId);
        if (customer == null) {
            return null;
        }
        String scope = permissionDao.findScope(userId, permission);
        boolean visible = SCOPE_ALL.equals(scope)
                || SCOPE_ASSIGNED.equals(scope) && Long.valueOf(userId).equals(customer.getSalesRepId());
        return visible ? customer : null;
    }
}
