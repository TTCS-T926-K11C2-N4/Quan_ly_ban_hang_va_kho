package com.oms.service;

import com.oms.model.SalesReport;
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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

// Báo cáo doanh số nhiều chiều (S8-06). Doanh số, sản lượng (đơn vị cơ sở) và biên lợi nhuận gộp lấy từ dòng hoá đơn
// chưa huỷ theo ngày lập hoá đơn; biên lợi nhuận dùng giá vốn chụp trên dòng hoá đơn nên chỉ hiện cho người xem được giá
// vốn. Số liệu chưa trừ chứng từ giảm trừ do trả hàng.
public class SalesReportService {

    private static final Map<String, String> KEY_EXPRESSIONS = Map.of(
            "REP", "COALESCE(u.full_name, 'Chưa phân công')",
            "REGION", "COALESCE(r.name, 'Chưa có khu vực')",
            "CUSTOMER", "c.name",
            "CATEGORY", "COALESCE(pc.name, 'Chưa phân nhóm')",
            "SKU", "CONCAT(p.sku, ' – ', p.name)");

    // salesRepId: chỉ đại lý do nhân viên đó phụ trách (null: tất cả)
    public SalesReport report(String dimension, LocalDate from, LocalDate to, boolean showMargin, Long salesRepId)
            throws SQLException {
        String dim = KEY_EXPRESSIONS.containsKey(dimension) ? dimension : "REP";
        LocalDate[] previous = SalesReport.previousPeriod(from, to);
        Map<String, BigDecimal[]> current = query(dim, from, to, salesRepId);
        Map<String, BigDecimal[]> before = query(dim, previous[0], previous[1], salesRepId);
        return SalesReport.build(dim, from, to, showMargin, current, before);
    }

    private static Map<String, BigDecimal[]> query(String dimension, LocalDate from, LocalDate to, Long salesRepId)
            throws SQLException {
        String key = KEY_EXPRESSIONS.get(dimension);
        String sql = "SELECT " + key + " AS k, SUM(i.line_total), SUM(i.qty_base),"
                + " SUM(i.line_total - i.qty_base * i.unit_cost_base_snapshot) FROM invoice_items i"
                + " JOIN invoices v ON v.id = i.invoice_id AND v.status <> 'CANCELLED'"
                + " JOIN customers c ON c.id = v.customer_id LEFT JOIN users u ON u.id = c.sales_rep_id"
                + " LEFT JOIN regions r ON r.id = c.region_id JOIN products p ON p.id = i.product_id"
                + " LEFT JOIN product_categories pc ON pc.id = p.category_id"
                + " WHERE v.issue_date BETWEEN ? AND ? AND (? IS NULL OR c.sales_rep_id = ?) GROUP BY k";
        Map<String, BigDecimal[]> result = new LinkedHashMap<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(from));
            statement.setDate(2, Date.valueOf(to));
            if (salesRepId == null) {
                statement.setNull(3, Types.BIGINT);
                statement.setNull(4, Types.BIGINT);
            } else {
                statement.setLong(3, salesRepId);
                statement.setLong(4, salesRepId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    result.put(resultSet.getString(1), new BigDecimal[]{resultSet.getBigDecimal(2),
                            resultSet.getBigDecimal(3), resultSet.getBigDecimal(4)});
                }
            }
        }
        return result;
    }

    public ByteArrayOutputStream export(SalesReport report) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Doanh số");
            CellStyle bold = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            bold.setFont(font);
            CellStyle money = workbook.createCellStyle();
            money.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
            CellStyle percent = workbook.createCellStyle();
            percent.setDataFormat(workbook.createDataFormat().getFormat("0.0"));
            String[] headers = report.isShowMargin()
                    ? new String[]{report.getDimensionLabel(), "Doanh số", "Sản lượng", "Lợi nhuận gộp",
                    "Biên lợi nhuận (%)", "Doanh số kỳ trước", "Thay đổi (%)"}
                    : new String[]{report.getDimensionLabel(), "Doanh số", "Sản lượng", "Doanh số kỳ trước",
                    "Thay đổi (%)"};
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
                header.getCell(i).setCellStyle(bold);
            }
            int index = 1;
            for (SalesReport.Row row : append(report)) {
                Row excel = sheet.createRow(index++);
                int c = 0;
                excel.createCell(c++).setCellValue(row.getName());
                excel.createCell(c).setCellValue(row.getRevenue().doubleValue());
                excel.getCell(c++).setCellStyle(money);
                excel.createCell(c++).setCellValue(row.getQty().doubleValue());
                if (report.isShowMargin()) {
                    excel.createCell(c).setCellValue(row.getProfit().doubleValue());
                    excel.getCell(c++).setCellStyle(money);
                    if (row.getMarginPercent() != null) {
                        excel.createCell(c).setCellValue(row.getMarginPercent().doubleValue());
                        excel.getCell(c).setCellStyle(percent);
                    }
                    c++;
                }
                excel.createCell(c).setCellValue(row.getPreviousRevenue().doubleValue());
                excel.getCell(c++).setCellStyle(money);
                if (row.getChangePercent() != null) {
                    excel.createCell(c).setCellValue(row.getChangePercent().doubleValue());
                    excel.getCell(c).setCellStyle(percent);
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

    private static java.util.List<SalesReport.Row> append(SalesReport report) {
        java.util.List<SalesReport.Row> rows = new java.util.ArrayList<>(report.getRows());
        rows.add(report.getTotal());
        return rows;
    }
}
