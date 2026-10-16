package com.oms.service;

import com.oms.dao.AuditLogDao;
import com.oms.dao.StockDao;
import com.oms.util.DbConnection;
import com.oms.util.JsonUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

// Khai báo tồn tối thiểu theo SKU và theo kho (S5-09), tính theo đơn vị cơ sở như sổ tồn.
// Chỉ đổi mức cảnh báo (stock_balances.min_stock_base), không đụng tới số tồn; ghi nhật ký cùng transaction.
public class MinStockService {

    // Số không âm, tối đa 12 chữ số nguyên và 3 chữ số thập phân như cột decimal(18,3); 0 = không cảnh báo
    private static final Pattern QTY_PATTERN = Pattern.compile("\\d{1,12}([.,]\\d{1,3})?");
    private static final String AUDIT_ENTITY = "INVENTORY";

    private final StockDao stockDao = new StockDao();
    private final AuditLogDao auditLogDao = new AuditLogDao();

    // Kết quả: lỗi (error != null), hoặc đã lưu kèm tên SKU / kho để báo lại
    public static final class Result {
        private final String error;
        private final String sku;
        private final String warehouseCode;
        private final BigDecimal minStock;

        private Result(String error, String sku, String warehouseCode, BigDecimal minStock) {
            this.error = error;
            this.sku = sku;
            this.warehouseCode = warehouseCode;
            this.minStock = minStock;
        }

        public String getError() {
            return error;
        }

        public String getSku() {
            return sku;
        }

        public String getWarehouseCode() {
            return warehouseCode;
        }

        public BigDecimal getMinStock() {
            return minStock;
        }
    }

    // null nếu text không hợp lệ
    static BigDecimal parseQty(String text) {
        if (text == null || !QTY_PATTERN.matcher(text.trim()).matches()) {
            return null;
        }
        return new BigDecimal(text.trim().replace(',', '.'));
    }

    // Result.error = "NOT_FOUND" khi SKU chưa có dòng tồn ở kho này
    public Result update(long actorUserId, long warehouseId, long productId, String rawQty, String ipAddress)
            throws SQLException {
        BigDecimal minStock = parseQty(rawQty);
        if (minStock == null) {
            return new Result("Tồn tối thiểu phải là số không âm, tối đa 3 chữ số thập phân (vd 120 hoặc 12,5).",
                    null, null, null);
        }
        try (Connection connection = DbConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                StockDao.MinStockRow row = stockDao.lockMinStock(connection, warehouseId, productId);
                if (row == null) {
                    connection.rollback();
                    return new Result("NOT_FOUND", null, null, null);
                }
                if (row.getMinStock().compareTo(minStock) != 0) {
                    stockDao.updateMinStock(connection, warehouseId, productId, minStock);
                    Map<String, Object> before = values(row, row.getMinStock());
                    Map<String, Object> after = values(row, minStock);
                    auditLogDao.insert(connection, actorUserId, "STOCK_MIN_UPDATE", AUDIT_ENTITY, productId,
                            JsonUtil.object(before), JsonUtil.object(after), null, ipAddress);
                }
                connection.commit();
                return new Result(null, row.getSku(), row.getWarehouseCode(), minStock);
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static Map<String, Object> values(StockDao.MinStockRow row, BigDecimal minStock) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("sku", row.getSku());
        values.put("warehouse", row.getWarehouseCode());
        values.put("minStockBase", minStock.stripTrailingZeros().toPlainString());
        return values;
    }
}
