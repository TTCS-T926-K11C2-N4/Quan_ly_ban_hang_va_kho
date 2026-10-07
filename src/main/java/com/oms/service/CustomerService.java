package com.oms.service;

import com.oms.dao.CustomerDao;
import com.oms.dao.PermissionDao;
import com.oms.model.Customer;

import java.sql.SQLException;
import java.util.List;

// Đại lý mà người dùng được xem theo data_scope của quyền: ALL mọi đại lý, ASSIGNED chỉ đại lý mình phụ trách
// (S3-06). OWN là tài khoản đại lý, không dùng các màn nội bộ này nên không thấy đại lý nào.
public class CustomerService {

    private static final String SCOPE_ALL = "ALL";
    private static final String SCOPE_ASSIGNED = "ASSIGNED";

    private final CustomerDao customerDao = new CustomerDao();
    private final PermissionDao permissionDao = new PermissionDao();

    public List<Customer> findVisibleCustomers(long userId, String permission) throws SQLException {
        String scope = permissionDao.findScope(userId, permission);
        if (SCOPE_ALL.equals(scope)) {
            return customerDao.findAll(null);
        }
        return SCOPE_ASSIGNED.equals(scope) ? customerDao.findAll(userId) : List.of();
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
