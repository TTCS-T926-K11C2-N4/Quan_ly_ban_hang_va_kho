package com.oms.service;

import com.oms.dao.DebtLedgerDao;
import com.oms.model.DebtAgingRow;
import com.oms.util.DateTimeUtil;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// Sổ công nợ theo đại lý với phân tích tuổi nợ 0–30, 31–60, 61–90, trên 90 ngày (S7-03).
// Tuổi nợ tính từ ngày lập hoá đơn; "quá hạn" là phần còn nợ của hoá đơn đã quá hạn thanh toán.
public class DebtLedgerService {

    private static final String[] HEADERS = {"Mã đại lý", "Đại lý", "Phải thu", "Đã thu", "Còn nợ", "0–30 ngày",
            "31–60 ngày", "61–90 ngày", "Trên 90 ngày", "Quá hạn"};

    private final DebtLedgerDao debtLedgerDao = new DebtLedgerDao();

    // Các dòng đại lý; phần tử cuối là dòng Tổng cộng khi có ít nhất một dòng
    public List<DebtAgingRow> rows(boolean overdueOnly) throws SQLException {
        return debtLedgerDao.findRows(DateTimeUtil.today(), overdueOnly);
    }

    public DebtAgingRow total(List<DebtAgingRow> rows) {
        return DebtAgingRow.total(rows);
    }

    public ByteArrayOutputStream export(boolean overdueOnly) throws SQLException, IOException {
        List<DebtAgingRow> rows = new ArrayList<>(rows(overdueOnly));
        DebtAgingRow total = DebtAgingRow.total(rows);
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sổ công nợ");
            CellStyle headerStyle = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);
            CellStyle moneyStyle = workbook.createCellStyle();
            moneyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
            CellStyle totalStyle = workbook.createCellStyle();
            totalStyle.cloneStyleFrom(moneyStyle);
            totalStyle.setFont(bold);
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
                header.getCell(i).setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (DebtAgingRow row : rows) {
                write(sheet.createRow(rowIndex++), row, moneyStyle, null);
            }
            write(sheet.createRow(rowIndex), total, totalStyle, headerStyle);
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out;
        }
    }

    private static void write(Row row, DebtAgingRow data, CellStyle moneyStyle, CellStyle labelStyle) {
        row.createCell(0).setCellValue(data.getCode());
        row.createCell(1).setCellValue(data.getName());
        if (labelStyle != null) {
            row.getCell(1).setCellStyle(labelStyle);
        }
        double[] values = {data.getReceivable().doubleValue(), data.getPaid().doubleValue(),
                data.getOutstanding().doubleValue(), data.getBucket0To30().doubleValue(),
                data.getBucket31To60().doubleValue(), data.getBucket61To90().doubleValue(),
                data.getBucketOver90().doubleValue(), data.getOverdue().doubleValue()};
        for (int i = 0; i < values.length; i++) {
            row.createCell(i + 2).setCellValue(values[i]);
            row.getCell(i + 2).setCellStyle(moneyStyle);
        }
    }
}
