package com.oms.service;

import com.oms.model.StockReport;
import com.oms.util.DbConnection;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Báo cáo tồn kho và vòng quay hàng (S8-07).
//  * Giá trị tồn = tồn thực tế x giá vốn, tổng hợp theo kho và theo nhóm hàng; chỉ hiện cho người được xem giá vốn.
//  * Số ngày tồn của SKU = tồn hiện tại / (hàng bán ra trong cửa sổ / số ngày của cửa sổ). Hàng bán ra là xuất kho trừ
//    hàng trả về; SKU còn tồn mà không bán ra, hoặc có số ngày tồn vượt ngưỡng, là hàng chậm luân chuyển.
public class StockReportService {

    public static final int DEFAULT_WINDOW_DAYS = 90;
    public static final int DEFAULT_SLOW_DAYS = 90;

    private static final String SQL = "SELECT p.sku, p.name, COALESCE(c.name, 'Chưa phân nhóm'), p.cost_price,"
            + " COALESCE(SUM(sb.qty_on_hand_base), 0),"
            + " (SELECT -COALESCE(SUM(m.qty_change_base), 0) FROM stock_movements m WHERE m.product_id = p.id"
            + " AND m.movement_type IN ('ISSUE', 'RETURN_IN') AND m.occurred_at >= DATE_SUB(UTC_TIMESTAMP(),"
            + " INTERVAL ? DAY)) FROM products p LEFT JOIN product_categories c ON c.id = p.category_id"
            + " JOIN stock_balances sb ON sb.product_id = p.id WHERE p.status = 'ACTIVE'"
            + " GROUP BY p.id, p.sku, p.name, c.name, p.cost_price HAVING SUM(sb.qty_on_hand_base) > 0"
            + " ORDER BY p.sku";
    private static final String WAREHOUSE_SQL = "SELECT w.name, COALESCE(SUM(sb.qty_on_hand_base * p.cost_price), 0)"
            + " FROM warehouses w JOIN stock_balances sb ON sb.warehouse_id = w.id"
            + " JOIN products p ON p.id = sb.product_id WHERE w.status = 'ACTIVE' GROUP BY w.id, w.name";
    private static final String CATEGORY_SQL = "SELECT COALESCE(c.name, 'Chưa phân nhóm'),"
            + " COALESCE(SUM(sb.qty_on_hand_base * p.cost_price), 0) FROM products p"
            + " LEFT JOIN product_categories c ON c.id = p.category_id JOIN stock_balances sb ON sb.product_id = p.id"
            + " GROUP BY c.id, c.name";

    public StockReport report(int windowDays, int slowDays, boolean showValue) throws SQLException {
        List<StockReport.Sku> skus = new ArrayList<>();
        Map<String, BigDecimal> warehouses = StockReport.newTotals();
        Map<String, BigDecimal> categories = StockReport.newTotals();
        try (Connection connection = DbConnection.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement(SQL)) {
                statement.setInt(1, windowDays);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        BigDecimal onHand = resultSet.getBigDecimal(5);
                        BigDecimal sold = resultSet.getBigDecimal(6);
                        BigDecimal days = StockReport.daysOfStock(onHand, sold, windowDays);
                        skus.add(new StockReport.Sku(resultSet.getString(1), resultSet.getString(2),
                                resultSet.getString(3), onHand, sold.max(BigDecimal.ZERO), days,
                                StockReport.isSlow(onHand, days, slowDays),
                                showValue ? onHand.multiply(resultSet.getBigDecimal(4)) : null));
                    }
                }
            }
            if (showValue) {
                try (PreparedStatement statement = connection.prepareStatement(WAREHOUSE_SQL);
                     ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        warehouses.put(resultSet.getString(1), resultSet.getBigDecimal(2));
                    }
                }
                try (PreparedStatement statement = connection.prepareStatement(CATEGORY_SQL);
                     ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        categories.put(resultSet.getString(1), resultSet.getBigDecimal(2));
                    }
                }
            }
        }
        skus.sort((x, y) -> {
            if (x.isSlow() != y.isSlow()) {
                return x.isSlow() ? -1 : 1;
            }
            return x.getSku().compareTo(y.getSku());
        });
        return StockReport.build(windowDays, slowDays, showValue, skus, warehouses, categories);
    }

    public ByteArrayOutputStream export(StockReport report) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Tồn kho và vòng quay");
            CellStyle bold = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            bold.setFont(font);
            CellStyle money = workbook.createCellStyle();
            money.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
            CellStyle qty = workbook.createCellStyle();
            qty.setDataFormat(workbook.createDataFormat().getFormat("#,##0.###"));
            String[] headers = report.isShowValue()
                    ? new String[]{"SKU", "Sản phẩm", "Nhóm hàng", "Tồn (đơn vị cơ sở)", "Bán ra trong kỳ",
                    "Số ngày tồn", "Chậm luân chuyển", "Giá trị tồn"}
                    : new String[]{"SKU", "Sản phẩm", "Nhóm hàng", "Tồn (đơn vị cơ sở)", "Bán ra trong kỳ",
                    "Số ngày tồn", "Chậm luân chuyển"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
                header.getCell(i).setCellStyle(bold);
            }
            int index = 1;
            for (StockReport.Sku sku : report.getSkus()) {
                Row row = sheet.createRow(index++);
                row.createCell(0).setCellValue(sku.getSku());
                row.createCell(1).setCellValue(sku.getName());
                row.createCell(2).setCellValue(sku.getCategory());
                row.createCell(3).setCellValue(sku.getOnHand().doubleValue());
                row.createCell(4).setCellValue(sku.getSold().doubleValue());
                row.getCell(3).setCellStyle(qty);
                row.getCell(4).setCellStyle(qty);
                if (sku.getDaysOfStock() == null) {
                    row.createCell(5).setCellValue("Không bán ra");
                } else {
                    row.createCell(5).setCellValue(sku.getDaysOfStock().doubleValue());
                }
                row.createCell(6).setCellValue(sku.isSlow() ? "Có" : "");
                if (report.isShowValue()) {
                    row.createCell(7).setCellValue(sku.getValue().doubleValue());
                    row.getCell(7).setCellStyle(money);
                }
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out;
        }
    }
}
