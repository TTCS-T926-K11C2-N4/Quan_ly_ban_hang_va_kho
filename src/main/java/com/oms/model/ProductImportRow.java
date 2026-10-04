package com.oms.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Một dòng sản phẩm đọc từ file Excel (S2-08): ô gốc để xuất báo cáo lỗi, form đã quy đổi nhóm hàng/đơn vị ra id,
// và nếu SKU đã có thì giá trị cũ để ghi nhật ký. Ô trống của SKU đã có nghĩa là giữ nguyên giá trị đang lưu.
public class ProductImportRow implements Serializable {

    private final int index;
    private final int excelRowNumber;
    private final List<String> cells;
    private final Long existingId;
    private final ProductForm form;
    private final BigDecimal costPrice;
    private final boolean costChanged;
    private final String categoryName;
    private final String unitName;
    private final String conversionText;
    private final Map<Long, BigDecimal> factors;
    private final boolean rewriteUnits;
    private final String oldValuesJson;
    private final List<String> errors = new ArrayList<>();
    private boolean imported;

    public ProductImportRow(int index, int excelRowNumber, List<String> cells, Long existingId, ProductForm form,
                            BigDecimal costPrice, boolean costChanged, String categoryName, String unitName,
                            String conversionText, Map<Long, BigDecimal> factors, boolean rewriteUnits,
                            String oldValuesJson) {
        this.index = index;
        this.excelRowNumber = excelRowNumber;
        this.cells = cells;
        this.existingId = existingId;
        this.form = form;
        this.costPrice = costPrice;
        this.costChanged = costChanged;
        this.categoryName = categoryName;
        this.unitName = unitName;
        this.conversionText = conversionText;
        this.factors = factors;
        this.rewriteUnits = rewriteUnits;
        this.oldValuesJson = oldValuesJson;
    }

    // STT trong danh sách xem trước (1, 2, 3...)
    public int getIndex() {
        return index;
    }

    // Số dòng thật trong Excel (dòng 1 là tiêu đề) để người dùng tìm lại dòng lỗi
    public int getExcelRowNumber() {
        return excelRowNumber;
    }

    public List<String> getCells() {
        return cells;
    }

    // SKU đã có trong hệ thống: dòng này cập nhật sản phẩm đó thay vì tạo mới (AC2)
    public boolean isUpdate() {
        return existingId != null;
    }

    public Long getExistingId() {
        return existingId;
    }

    public ProductForm getForm() {
        return form;
    }

    // Ô SKU sau khi viết hoa; rỗng nếu bỏ trống
    public String getSku() {
        return form.getSku() == null ? "" : form.getSku();
    }

    // null = không ghi giá vốn (người nhập không có quyền, hoặc SKU đã có mà ô trống)
    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public boolean isCostChanged() {
        return costChanged;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getUnitName() {
        return unitName;
    }

    // Quy đổi sẽ lưu, vd "Lốc = 6, Thùng = 24"; rỗng nếu không có
    public String getConversionText() {
        return conversionText;
    }

    // Hệ số theo id đơn vị sẽ lưu vào product_units (không gồm đơn vị cơ sở)
    public Map<Long, BigDecimal> getFactors() {
        return factors;
    }

    // Có ghi lại bảng product_units không: khi thêm mới, khi ô Quy đổi có giá trị, hoặc khi đổi đơn vị cơ sở
    public boolean isRewriteUnits() {
        return rewriteUnits;
    }

    public String getOldValuesJson() {
        return oldValuesJson;
    }

    public List<String> getErrors() {
        return errors;
    }

    // Mỗi lỗi đã kết thúc bằng dấu chấm nên chỉ cần nối bằng khoảng trắng
    public String getErrorText() {
        return String.join(" ", errors);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public void addError(String error) {
        errors.add(error);
    }

    public boolean isImported() {
        return imported;
    }

    public void markImported() {
        imported = true;
    }
}
