package com.oms.service;

import com.oms.dao.StockLedgerDao;
import com.oms.dao.WarehouseDao;
import com.oms.model.PageResult;
import com.oms.model.SelectOption;
import com.oms.model.StockLedgerFilter;
import com.oms.model.StockLedgerRow;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

// Sổ tồn kho theo từng SKU và từng kho (S5-05): tồn thực tế, đang giữ chỗ, tồn khả dụng tách bạch; lọc theo kho,
// nhóm hàng, trạng thái tồn; tìm nhanh theo mã hoặc tên sản phẩm.
public class StockLedgerService {

    public static final int PAGE_SIZE = 10;

    private static final String[] EXPORT_HEADERS = {"SKU", "Sản phẩm", "Nhóm hàng", "Mã kho", "Kho", "Tồn thực tế",
        "Đang giữ chỗ", "Tồn khả dụng", "Tồn tối thiểu", "Đơn vị cơ sở", "Trạng thái"};

    private final StockLedgerDao ledgerDao = new StockLedgerDao();
    private final WarehouseDao warehouseDao = new WarehouseDao();

    // Tên tệp xuất (S8-08): chứa mã kho đang lọc (hoặc "tat-ca-kho") và ngày xuất yyyyMMdd
    public String exportFileName(StockLedgerFilter filter, String dateText) throws SQLException {
        String scope = "tat-ca-kho";
        if (filter.getWarehouseId() != null) {
            com.oms.model.Warehouse warehouse = warehouseDao.findById(filter.getWarehouseId());
            if (warehouse != null) {
                scope = warehouse.getCode().replaceAll("[^A-Za-z0-9_-]", "");
            }
        }
        return "so-ton-kho-" + scope + "-" + dateText + ".xlsx";
    }

    // Ba thẻ số liệu đầu trang, theo bộ lọc kho / nhóm hàng / từ khoá đang chọn
    public record Summary(long total, long out, long low) {
        public long getTotal() {
            return total;
        }

        public long getOut() {
            return out;
        }

        public long getLow() {
            return low;
        }
    }

    public PageResult<StockLedgerRow> search(StockLedgerFilter filter, int requestedPage) throws SQLException {
        long total = ledgerDao.count(filter);
        int totalPages = (int) Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(Math.max(1, requestedPage), totalPages);
        return new PageResult<>(ledgerDao.findPage(filter, (page - 1) * PAGE_SIZE, PAGE_SIZE), page, PAGE_SIZE, total);
    }

    public Summary summary(StockLedgerFilter filter) throws SQLException {
        long[] counts = ledgerDao.countSummary(filter);
        return new Summary(counts[0], counts[1], counts[2]);
    }

    public List<SelectOption> getCategories() throws SQLException {
        return ledgerDao.findCategories();
    }

    // File Excel sổ tồn theo bộ lọc đang chọn, số lượng theo đơn vị cơ sở
    public ByteArrayOutputStream export(StockLedgerFilter filter) throws SQLException, IOException {
        List<StockLedgerRow> rows = ledgerDao.findPage(filter, 0, 0);
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sổ tồn kho");
            CellStyle headerStyle = workbook.createCellStyle();
            Font bold = workbook.createFont();
            bold.setBold(true);
            headerStyle.setFont(bold);
            CellStyle qtyStyle = workbook.createCellStyle();
            qtyStyle.setDataFormat(workbook.createDataFormat().getFormat("#,##0.###"));
            Row header = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                header.createCell(i).setCellValue(EXPORT_HEADERS[i]);
                header.getCell(i).setCellStyle(headerStyle);
            }
            int rowIndex = 1;
            for (StockLedgerRow stock : rows) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(stock.getSku());
                row.createCell(1).setCellValue(stock.getProductName());
                row.createCell(2).setCellValue(stock.getCategoryName());
                row.createCell(3).setCellValue(stock.getWarehouseCode());
                row.createCell(4).setCellValue(stock.getWarehouseName());
                row.createCell(5).setCellValue(stock.getOnHand().doubleValue());
                row.createCell(6).setCellValue(stock.getReserved().doubleValue());
                row.createCell(7).setCellValue(stock.getAvailable().doubleValue());
                row.createCell(8).setCellValue(stock.getMinStock().doubleValue());
                for (int i = 5; i <= 8; i++) {
                    row.getCell(i).setCellStyle(qtyStyle);
                }
                row.createCell(9).setCellValue(stock.getBaseUnitName());
                row.createCell(10).setCellValue(stock.getLevel().getLabel());
            }
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out;
        }
    }
}
