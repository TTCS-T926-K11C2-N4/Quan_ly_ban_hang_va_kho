package com.oms.model;

import java.io.Serializable;
import java.util.List;

// Các dòng sản phẩm đọc được từ một file Excel, giữ trong session từ bước xem trước tới bước nhập (S2-08)
public class ProductImportBatch implements Serializable {

    private final String fileName;
    private final List<ProductImportRow> rows;

    public ProductImportBatch(String fileName, List<ProductImportRow> rows) {
        this.fileName = fileName;
        this.rows = rows;
    }

    public String getFileName() {
        return fileName;
    }

    public List<ProductImportRow> getRows() {
        return rows;
    }

    public int getTotalCount() {
        return rows.size();
    }

    public long getValidCount() {
        return rows.stream().filter(ProductImportRow::isValid).count();
    }

    public long getNewCount() {
        return rows.stream().filter(row -> row.isValid() && !row.isUpdate()).count();
    }

    public long getUpdateCount() {
        return rows.stream().filter(row -> row.isValid() && row.isUpdate()).count();
    }

    public long getErrorCount() {
        return getTotalCount() - getValidCount();
    }

    public long getCreatedCount() {
        return rows.stream().filter(row -> row.isImported() && !row.isUpdate()).count();
    }

    public long getUpdatedCount() {
        return rows.stream().filter(row -> row.isImported() && row.isUpdate()).count();
    }

    public List<ProductImportRow> getErrorRows() {
        return rows.stream().filter(row -> !row.isValid()).toList();
    }
}
