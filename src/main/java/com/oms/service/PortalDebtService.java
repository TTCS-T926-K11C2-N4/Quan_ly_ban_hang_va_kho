package com.oms.service;

import com.oms.dao.CustomerCreditDao;
import com.oms.dao.InvoiceDao;
import com.oms.dao.PortalOrderDao;
import com.oms.model.CustomerCredit;
import com.oms.model.Invoice;
import com.oms.util.DateTimeUtil;

import java.sql.SQLException;
import java.util.List;

// Đại lý xem công nợ và hoá đơn của chính mình trên cổng (S7-07). Đại lý lấy từ tài khoản đăng nhập
// (users.customer_id), không bao giờ từ tham số trang, nên không xem được hoá đơn của đại lý khác.
public class PortalDebtService {

    private static final int LIST_LIMIT = 200;

    private final PortalOrderDao portalOrderDao = new PortalOrderDao();
    private final InvoiceDao invoiceDao = new InvoiceDao();
    private final CustomerCreditDao creditDao = new CustomerCreditDao();

    // Tài khoản chưa gắn đại lý thì không có hoá đơn nào
    public List<Invoice> findInvoices(long userId) throws SQLException {
        Long customerId = portalOrderDao.findCustomerId(userId);
        return customerId == null ? List.of()
                : invoiceDao.findInvoices(customerId, null, false, DateTimeUtil.today(), LIST_LIMIT);
    }

    // null nếu tài khoản chưa gắn đại lý
    public CustomerCredit findCredit(long userId) throws SQLException {
        Long customerId = portalOrderDao.findCustomerId(userId);
        return customerId == null ? null : creditDao.find(customerId, DateTimeUtil.today());
    }

    // null nếu không có hoá đơn này hoặc hoá đơn của đại lý khác
    public Invoice findInvoice(long userId, long invoiceId) throws SQLException {
        Long customerId = portalOrderDao.findCustomerId(userId);
        if (customerId == null) {
            return null;
        }
        Invoice invoice = invoiceDao.findDetail(invoiceId);
        return invoice != null && invoice.getCustomerId() == customerId ? invoice : null;
    }
}
