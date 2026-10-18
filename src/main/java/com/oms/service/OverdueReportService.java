package com.oms.service;

import com.oms.model.OverdueReport;
import com.oms.util.DateTimeUtil;
import com.oms.util.DbConnection;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Báo cáo công nợ quá hạn theo tuổi nợ (S8-09). Tuổi nợ tính từ hạn thanh toán của hoá đơn còn nợ: 1–30, 31–60, 61–90,
// trên 90 ngày quá hạn. Tổng hợp theo nhóm tuổi và theo nhân viên phụ trách; đại lý xếp theo mức rủi ro.
public class OverdueReportService {

    private static final String SQL = "SELECT c.id, c.code, c.name, u.full_name,"
            + " SUM(v.outstanding_amount),"
            + " SUM(IF(DATEDIFF(?, v.due_date) BETWEEN 1 AND 30, v.outstanding_amount, 0)),"
            + " SUM(IF(DATEDIFF(?, v.due_date) BETWEEN 31 AND 60, v.outstanding_amount, 0)),"
            + " SUM(IF(DATEDIFF(?, v.due_date) BETWEEN 61 AND 90, v.outstanding_amount, 0)),"
            + " SUM(IF(DATEDIFF(?, v.due_date) > 90, v.outstanding_amount, 0)),"
            + " MAX(DATEDIFF(?, v.due_date)) FROM invoices v JOIN customers c ON c.id = v.customer_id"
            + " LEFT JOIN users u ON u.id = c.sales_rep_id WHERE v.status IN ('ISSUED', 'PARTIALLY_PAID')"
            + " AND v.outstanding_amount > 0 AND v.due_date < ? GROUP BY c.id, c.code, c.name, u.full_name";

    private static final String[] HEADERS = {"Mã đại lý", "Đại lý", "Nhân viên phụ trách", "Quá hạn", "1–30 ngày",
            "31–60 ngày", "61–90 ngày", "Trên 90 ngày", "Quá hạn lâu nhất (ngày)", "Mức rủi ro"};

    public OverdueReport report() throws SQLException {
        LocalDate today = DateTimeUtil.today();
        List<OverdueReport.Row> rows = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(SQL)) {
            Date date = Date.valueOf(today);
            for (int i = 1; i <= 6; i++) {
                statement.setDate(i, date);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    rows.add(new OverdueReport.Row(resultSet.getLong(1), resultSet.getString(2),
                            resultSet.getString(3), resultSet.getString(4), resultSet.getBigDecimal(5),
                            resultSet.getBigDecimal(6), resultSet.getBigDecimal(7), resultSet.getBigDecimal(8),
                            resultSet.getBigDecimal(9), resultSet.getInt(10)));
                }
            }
        }
        return OverdueReport.build(rows);
    }

    public ByteArrayOutputStream export(OverdueReport report) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Công nợ quá hạn");
            CellStyle bold = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            bold.setFont(font);
            CellStyle money = workbook.createCellStyle();
            money.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
                header.getCell(i).setCellStyle(bold);
            }
            int index = 1;
            for (OverdueReport.Row r : report.getRows()) {
                Row row = sheet.createRow(index++);
                row.createCell(0).setCellValue(r.getCode());
                row.createCell(1).setCellValue(r.getName());
                row.createCell(2).setCellValue(r.getRepName());
                double[] values = {r.getOverdue().doubleValue(), r.getBucket1To30().doubleValue(),
                        r.getBucket31To60().doubleValue(), r.getBucket61To90().doubleValue(),
                        r.getBucketOver90().doubleValue()};
                for (int i = 0; i < values.length; i++) {
                    row.createCell(3 + i).setCellValue(values[i]);
                    row.getCell(3 + i).setCellStyle(money);
                }
                row.createCell(8).setCellValue(r.getOldestDays());
                row.createCell(9).setCellValue(r.getRiskLabel());
            }
            Row total = sheet.createRow(index++);
            total.createCell(1).setCellValue("Tổng cộng");
            total.getCell(1).setCellStyle(bold);
            double[] sums = {report.getTotal().doubleValue(), report.getBucket1To30().doubleValue(),
                    report.getBucket31To60().doubleValue(), report.getBucket61To90().doubleValue(),
                    report.getBucketOver90().doubleValue()};
            for (int i = 0; i < sums.length; i++) {
                total.createCell(3 + i).setCellValue(sums[i]);
                total.getCell(3 + i).setCellStyle(money);
            }
            index++;
            Row repHeader = sheet.createRow(index++);
            repHeader.createCell(1).setCellValue("Theo nhân viên phụ trách");
            repHeader.getCell(1).setCellStyle(bold);
            for (OverdueReport.RepTotal rep : report.getRepTotals()) {
                Row row = sheet.createRow(index++);
                row.createCell(1).setCellValue(rep.getName());
                row.createCell(3).setCellValue(rep.getAmount().doubleValue());
                row.getCell(3).setCellStyle(money);
            }
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out;
        }
    }
}
