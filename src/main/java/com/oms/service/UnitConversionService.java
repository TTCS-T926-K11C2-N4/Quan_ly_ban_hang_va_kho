package com.oms.service;

import com.oms.dao.ProductDao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;

// Quy đổi số lượng về đơn vị cơ sở (S2-07). Đơn hàng, phiếu nhập/xuất/chuyển/kiểm kê... (EP-04, EP-05) phải gọi
// toBase khi ghi dòng chứng từ và lưu CẢ hai giá trị trả về: unit_factor (chụp lại hệ số lúc ghi) và qty_base.
// Sổ tồn, công nợ chỉ tính theo qty_base nên đổi hệ số quy đổi sau này không làm sai giao dịch đã ghi.
public class UnitConversionService {

    // Khớp cột decimal(18,4) của factor_to_base / unit_factor và decimal(18,3) của qty_base
    static final int FACTOR_SCALE = 4;
    static final int QTY_SCALE = 3;
    private static final int FACTOR_MAX_INTEGER_DIGITS = 14;

    private final ProductDao productDao = new ProductDao();

    // Kết quả quy đổi của một dòng chứng từ
    public static final class Converted {
        private final BigDecimal unitFactor;
        private final BigDecimal qtyBase;

        Converted(BigDecimal unitFactor, BigDecimal qtyBase) {
            this.unitFactor = unitFactor;
            this.qtyBase = qtyBase;
        }

        public BigDecimal getUnitFactor() {
            return unitFactor;
        }

        public BigDecimal getQtyBase() {
            return qtyBase;
        }
    }

    // Báo khi SKU không khai báo đơn vị đang dùng (chứng từ không được ghi)
    public static class UnitNotConfiguredException extends Exception {
        public UnitNotConfiguredException(String message) {
            super(message);
        }
    }

    // Hệ số đọc theo cấu hình hiện tại của SKU (đơn vị cơ sở = 1)
    public Converted toBase(long productId, long unitId, BigDecimal qty)
            throws SQLException, UnitNotConfiguredException {
        BigDecimal factor = productDao.findFactor(productId, unitId);
        if (factor == null) {
            throw new UnitNotConfiguredException("Sản phẩm chưa khai báo đơn vị tính này.");
        }
        return convert(qty, factor);
    }

    static Converted convert(BigDecimal qty, BigDecimal factor) {
        return new Converted(factor.setScale(FACTOR_SCALE, RoundingMode.HALF_UP),
                qty.multiply(factor).setScale(QTY_SCALE, RoundingMode.HALF_UP));
    }

    // Hệ số nhập ở form: số dương tối đa 4 số lẻ, dấu thập phân là "." hoặc ",", khác 1 (bằng 1 thì trùng đơn vị cơ sở).
    // null nếu không hợp lệ.
    static BigDecimal parseFactor(String value) {
        if (value == null || !value.matches("\\d{1," + FACTOR_MAX_INTEGER_DIGITS + "}([.,]\\d{1," + FACTOR_SCALE + "})?")) {
            return null;
        }
        BigDecimal factor = new BigDecimal(value.replace(',', '.'));
        return factor.signum() > 0 && factor.compareTo(BigDecimal.ONE) != 0 ? factor : null;
    }
}
