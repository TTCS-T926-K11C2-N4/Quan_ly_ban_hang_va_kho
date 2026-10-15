package com.oms.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Đổi lô của dòng soạn hàng (S6-04): hệ thống gợi ý lô FEFO lúc lập phiếu (PickListDao); thủ kho được chọn lô khác
// còn đủ số lượng nhưng phải ghi lý do (pick_list_items.lot_override_reason).
public class PickLotDao {

    private final PickListDao pickListDao = new PickListDao();

    // Dòng soạn khoá để đổi lô; null nếu không có dòng thuộc phiếu
    public Line lockLine(Connection connection, long pickListId, long lineId) throws SQLException {
        String sql = "SELECT pi.lot_id, pi.product_id, pi.qty_required_base, pi.qty_picked_base, pl.pick_no,"
                + " pl.status, pl.warehouse_id, p.sku, lo.lot_no FROM pick_list_items pi"
                + " JOIN pick_lists pl ON pl.id = pi.pick_list_id JOIN products p ON p.id = pi.product_id"
                + " JOIN lots lo ON lo.id = pi.lot_id WHERE pi.id = ? AND pi.pick_list_id = ? FOR UPDATE";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, lineId);
            statement.setLong(2, pickListId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? new Line(lineId, resultSet.getLong(1), resultSet.getLong(2),
                        resultSet.getBigDecimal(3), resultSet.getBigDecimal(4), resultSet.getString(5),
                        resultSet.getString(6), resultSet.getLong(7), resultSet.getString(8),
                        resultSet.getString(9)) : null;
            }
        }
    }

    public void changeLot(Connection connection, long lineId, long lotId, String reason) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE pick_list_items SET lot_id = ?, lot_override_reason = ? WHERE id = ?")) {
            statement.setLong(1, lotId);
            statement.setString(2, reason);
            statement.setLong(3, lineId);
            statement.executeUpdate();
        }
    }

    // Lô khác còn đủ số lượng cho từng dòng chưa soạn của phiếu (khoá khi hiện form đổi lô)
    public Map<Long, List<PickListDao.LotStock>> findCandidates(Connection connection, long pickListId,
                                                               LocalDate today) throws SQLException {
        String sql = "SELECT pi.id, pi.lot_id, pi.product_id, pi.qty_required_base, pl.warehouse_id"
                + " FROM pick_list_items pi JOIN pick_lists pl ON pl.id = pi.pick_list_id"
                + " WHERE pi.pick_list_id = ? AND pi.qty_picked_base IS NULL ORDER BY pi.id";
        Map<Long, List<PickListDao.LotStock>> result = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, pickListId);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<Object[]> lines = new ArrayList<>();
                while (resultSet.next()) {
                    lines.add(new Object[] {resultSet.getLong(1), resultSet.getLong(2), resultSet.getLong(3),
                        resultSet.getBigDecimal(4), resultSet.getLong(5)});
                }
                for (Object[] line : lines) {
                    long lineId = (Long) line[0];
                    long currentLot = (Long) line[1];
                    BigDecimal required = (BigDecimal) line[3];
                    List<PickListDao.LotStock> options = new ArrayList<>();
                    for (PickListDao.LotStock lot : pickListDao.findPickableLots(connection, (Long) line[4],
                            (Long) line[2], today)) {
                        if (lot.lotId() != currentLot && lot.free().compareTo(required) >= 0) {
                            options.add(lot);
                        }
                    }
                    if (!options.isEmpty()) {
                        result.put(lineId, options);
                    }
                }
            }
        }
        return result;
    }

    public record Line(long id, long lotId, long productId, BigDecimal qtyRequired, BigDecimal qtyPicked,
                       String pickNo, String status, long warehouseId, String sku, String lotNo) {
    }
}
