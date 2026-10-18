package com.oms.service;

import com.oms.dao.NotificationDao;
import com.oms.dao.OverdueDeliveryDao;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;

import java.sql.Connection;
import java.sql.SQLException;

// Báo nhân viên kinh doanh phụ trách đại lý khi đơn đã quá ngày giao mong muốn mà chưa giao xong (đơn Đã duyệt,
// Đang soạn hàng hoặc Đã xuất kho). Không đổi trạng thái đơn, không tự huỷ hay hẹn lại; mỗi đơn báo một lần.
public class OverdueDeliveryService {

    private final OverdueDeliveryDao overdueDao = new OverdueDeliveryDao();
    private final NotificationDao notificationDao = new NotificationDao();

    // Trả về số đơn vừa báo
    public int notifyOverdue() throws SQLException {
        int notified = 0;
        for (OverdueDeliveryDao.OverdueOrder order : overdueDao.findUnnotified(DateTimeUtil.today())) {
            try (Connection connection = DbConnection.getConnection()) {
                notificationDao.insert(connection, order.salesRepId(), OverdueDeliveryDao.NOTIFICATION_TYPE,
                        "Đơn " + order.orderNo() + " đã quá ngày giao mong muốn",
                        "Đơn " + order.orderNo() + " của " + order.customerName() + " có ngày giao mong muốn "
                                + DateTimeUtil.formatDay(order.requestedDate()) + " nhưng chưa giao xong."
                                + " Vui lòng liên hệ kho và đại lý để xử lý.",
                        "ORDER", order.orderId(), false);
                notified++;
            }
        }
        return notified;
    }
}
