package com.oms.service;

import com.oms.model.DebtDetail;
import com.oms.model.SelectOption;
import com.oms.util.DateTimeUtil;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Sổ chi tiết công nợ một đại lý (S8-10): toàn bộ phát sinh tăng giảm theo thời gian kèm số dư luỹ kế, lọc theo khoảng
// thời gian, xuất PDF (trang in) và Excel để gửi đại lý đối chiếu khi họ thắc mắc về số dư.
public class DebtDetailService {

    private static final String[] HEADERS = {"Ngày", "Loại", "Nội dung", "Tăng nợ", "Giảm nợ", "Số dư luỹ kế"};

    public List<SelectOption> customers() throws SQLException {
        List<SelectOption> result = new ArrayList<>();
        try (Connection connection = DbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, code, name FROM customers ORDER BY name");
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                result.add(new SelectOption(resultSet.getLong(1), resultSet.getString(2), resultSet.getString(3)));
            }
        }
        return result;
    }

    // null nếu không có đại lý. from, to đã được chuẩn hoá bởi người gọi (from <= to)
    public DebtDetail find(long customerId, LocalDate from, LocalDate to) throws SQLException {
        try (Connection connection = DbConnection.getConnection()) {
            String code;
            String name;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT code, name FROM customers WHERE id = ?")) {
                statement.setLong(1, customerId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    code = resultSet.getString(1);
                    name = resultSet.getString(2);
                }
            }
            BigDecimal opening;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT COALESCE(SUM(debit_amount - credit_amount), 0) FROM ar_ledger_entries"
                            + " WHERE customer_id = ? AND entry_date < ?")) {
                statement.setLong(1, customerId);
                statement.setDate(2, Date.valueOf(from));
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    opening = resultSet.getBigDecimal(1);
                }
            }
            List<DebtDetail.Entry> entries = new ArrayList<>();
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT entry_date, entry_type, note, debit_amount, credit_amount FROM ar_ledger_entries"
                            + " WHERE customer_id = ? AND entry_date BETWEEN ? AND ? ORDER BY entry_date, id")) {
                statement.setLong(1, customerId);
                statement.setDate(2, Date.valueOf(from));
                statement.setDate(3, Date.valueOf(to));
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        entries.add(new DebtDetail.Entry(resultSet.getObject(1, LocalDate.class),
                                resultSet.getString(2), resultSet.getString(3), resultSet.getBigDecimal(4),
                                resultSet.getBigDecimal(5)));
                    }
                }
            }
            return DebtDetail.build(customerId, code, name, from, to, opening, entries);
        }
    }

    public ByteArrayOutputStream export(DebtDetail detail) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sổ chi tiết công nợ");
            CellStyle bold = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            bold.setFont(font);
            CellStyle money = workbook.createCellStyle();
            money.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
            CellStyle moneyBold = workbook.createCellStyle();
            moneyBold.cloneStyleFrom(money);
            moneyBold.setFont(font);
            int rowIndex = 0;
            Row title = sheet.createRow(rowIndex++);
            title.createCell(0).setCellValue("Sổ chi tiết công nợ: " + detail.getCustomerName() + " ("
                    + detail.getCustomerCode() + ") · Kỳ " + detail.getPeriodText());
            title.getCell(0).setCellStyle(bold);
            Row header = sheet.createRow(rowIndex++);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
                header.getCell(i).setCellStyle(bold);
            }
            Row opening = sheet.createRow(rowIndex++);
            opening.createCell(0).setCellValue(DateTimeUtil.formatDay(detail.getFrom()));
            opening.createCell(2).setCellValue("Số dư đầu kỳ");
            opening.createCell(5).setCellValue(detail.getOpening().doubleValue());
            opening.getCell(5).setCellStyle(money);
            for (DebtDetail.Row line : detail.getRows()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(line.getDateText());
                row.createCell(1).setCellValue(line.getTypeLabel());
                row.createCell(2).setCellValue(line.getNote() == null ? "" : line.getNote());
                row.createCell(3).setCellValue(line.getDebit().doubleValue());
                row.createCell(4).setCellValue(line.getCredit().doubleValue());
                row.createCell(5).setCellValue(line.getBalance().doubleValue());
                for (int i = 3; i <= 5; i++) {
                    row.getCell(i).setCellStyle(money);
                }
            }
            Row total = sheet.createRow(rowIndex);
            total.createCell(2).setCellValue("Tổng phát sinh / số dư cuối kỳ");
            total.getCell(2).setCellStyle(bold);
            total.createCell(3).setCellValue(detail.getTotalDebit().doubleValue());
            total.createCell(4).setCellValue(detail.getTotalCredit().doubleValue());
            total.createCell(5).setCellValue(detail.getClosing().doubleValue());
            for (int i = 3; i <= 5; i++) {
                total.getCell(i).setCellStyle(moneyBold);
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
