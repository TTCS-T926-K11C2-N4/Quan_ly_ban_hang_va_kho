package com.oms.model;

import java.io.Serializable;
import java.util.List;

// Các dòng đọc được từ một file Excel, giữ trong session từ bước xem trước tới bước nhập (S2-01)
public class ImportBatch implements Serializable {

    private final String fileName;
    private final List<ImportRow> rows;

    public ImportBatch(String fileName, List<ImportRow> rows) {
        this.fileName = fileName;
        this.rows = rows;
    }

    public String getFileName() {
        return fileName;
    }

    public List<ImportRow> getRows() {
        return rows;
    }

    public int getTotalCount() {
        return rows.size();
    }

    public long getValidCount() {
        return rows.stream().filter(ImportRow::isValid).count();
    }

    // Nhập người dùng không cập nhật tài khoản có sẵn (tài khoản trùng là dòng lỗi); giữ ô này cho giống thiết kế
    // dùng chung với Nhập sản phẩm (S2-08)
    public long getUpdateCount() {
        return 0;
    }

    public long getErrorCount() {
        return getTotalCount() - getValidCount();
    }

    public long getCreatedCount() {
        return rows.stream().filter(ImportRow::isCreated).count();
    }

    public List<ImportRow> getErrorRows() {
        return rows.stream().filter(row -> !row.isValid()).toList();
    }
}
