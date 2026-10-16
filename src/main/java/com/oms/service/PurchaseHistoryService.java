package com.oms.service;

import com.oms.dao.PurchaseHistoryDao;
import com.oms.model.Customer;
import com.oms.model.Permission;
import com.oms.model.PurchaseHistory;
import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Lịch sử mua hàng của đại lý khi gõ đơn (S4-04). Phạm vi như tạo đơn (ORDER_MANAGE): Nhân viên kinh doanh chỉ xem
// được đại lý mình phụ trách, đại lý khác trả null (404).
public class PurchaseHistoryService {

    public static final int MONTHS = 3;

    private final PurchaseHistoryDao historyDao = new PurchaseHistoryDao();
    private final CustomerService customerService = new CustomerService();

    // null nếu không có đại lý này hoặc người dùng không được tạo đơn cho đại lý đó
    public PurchaseHistory find(long userId, long customerId) throws SQLException {
        Customer customer = customerService.findVisible(userId, Permission.ORDER_MANAGE, customerId);
        if (customer == null) {
            return null;
        }
        LocalDateTime since = DateTimeUtil.startOfDayUtc(DateTimeUtil.today().minusMonths(MONTHS));
        return summarize(historyDao.findRows(customer.getId(), since));
    }

    // Không đọc CSDL nên test được. rows: mới nhất trước, các dòng cùng đơn đứng liền nhau.
    static PurchaseHistory summarize(List<PurchaseHistoryDao.Row> rows) {
        if (rows.isEmpty()) {
            return new PurchaseHistory(List.of(), null, null, List.of());
        }
        PurchaseHistoryDao.Row latest = rows.get(0);
        List<PurchaseHistory.Line> lastLines = new ArrayList<>();
        for (PurchaseHistoryDao.Row row : rows) {
            if (row.getOrderId() == latest.getOrderId()) {
                lastLines.add(new PurchaseHistory.Line(row.getProductId(), row.getSku(), row.getName(),
                        row.getUnitId(), row.getUnitName(), row.getQty()));
            }
        }

        Map<Long, List<PurchaseHistoryDao.Row>> byProduct = new LinkedHashMap<>();
        for (PurchaseHistoryDao.Row row : rows) {
            byProduct.computeIfAbsent(row.getProductId(), key -> new ArrayList<>()).add(row);
        }
        List<PurchaseHistory.Item> items = new ArrayList<>();
        for (List<PurchaseHistoryDao.Row> productRows : byProduct.values()) {
            items.add(item(productRows));
        }
        // Mặt hàng mua nhiều lần nhất lên đầu, bằng nhau thì mua gần đây hơn
        items.sort((a, b) -> b.getOrderCount() != a.getOrderCount()
                ? Integer.compare(b.getOrderCount(), a.getOrderCount()) : b.getLastDate().compareTo(a.getLastDate()));
        return new PurchaseHistory(items, latest.getOrderNo(), day(latest), lastLines);
    }

    // Bình quân theo đơn vị mua nhiều đơn nhất (bằng nhau thì đơn vị mua gần đây hơn); không quy đổi giữa các đơn vị
    private static PurchaseHistory.Item item(List<PurchaseHistoryDao.Row> productRows) {
        Map<Long, BigDecimal> sum = new HashMap<>();
        Map<Long, Integer> lines = new HashMap<>();
        Map<Long, Set<Long>> orders = new HashMap<>();
        for (PurchaseHistoryDao.Row row : productRows) {
            sum.merge(row.getUnitId(), row.getQty(), BigDecimal::add);
            lines.merge(row.getUnitId(), 1, Integer::sum);
            orders.computeIfAbsent(row.getUnitId(), key -> new HashSet<>()).add(row.getOrderId());
        }
        PurchaseHistoryDao.Row best = productRows.get(0);
        for (PurchaseHistoryDao.Row row : productRows) {
            if (orders.get(row.getUnitId()).size() > orders.get(best.getUnitId()).size()) {
                best = row;
            }
        }
        BigDecimal avg = sum.get(best.getUnitId())
                .divide(BigDecimal.valueOf(lines.get(best.getUnitId())), 1, RoundingMode.HALF_UP);
        Set<Long> allOrders = new HashSet<>();
        productRows.forEach(row -> allOrders.add(row.getOrderId()));
        PurchaseHistoryDao.Row first = productRows.get(0);
        return new PurchaseHistory.Item(first.getProductId(), first.getSku(), first.getName(), best.getUnitId(),
                best.getUnitName(), allOrders.size(), avg, day(first));
    }

    private static LocalDate day(PurchaseHistoryDao.Row row) {
        return DateTimeUtil.toDay(row.getCreatedAt());
    }
}
