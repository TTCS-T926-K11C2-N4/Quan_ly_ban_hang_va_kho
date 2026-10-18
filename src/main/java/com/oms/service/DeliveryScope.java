package com.oms.service;

import com.oms.dao.PermissionDao;
import com.oms.model.Permission;

import java.sql.SQLException;

// Phạm vi xem phiếu soạn, phiếu xuất và giao hàng theo quyền DELIVERY_VIEW (S6-03, S6-05, S6-08):
// ALL xem mọi đơn; ASSIGNED (nhân viên kinh doanh) chỉ đơn của đại lý mình phụ trách (salesRepId là người cần lọc,
// null khi xem mọi đơn); OWN (tài khoản đại lý) hoặc không có quyền thì không xem các màn nội bộ này.
public record DeliveryScope(boolean allowed, Long salesRepId) {

    public static DeliveryScope of(PermissionDao permissionDao, long userId) throws SQLException {
        String scope = permissionDao.findScope(userId, Permission.DELIVERY_VIEW);
        return new DeliveryScope("ALL".equals(scope) || "ASSIGNED".equals(scope),
                "ASSIGNED".equals(scope) ? userId : null);
    }
}
