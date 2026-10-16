package com.oms.model;

import com.oms.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

// Tồn khả dụng cho từng dòng đơn đang gõ (S4-03), tính ở kho phục vụ của đại lý. Khả dụng = tồn thực tế − đang giữ chỗ
// cho đơn khác. Một SKU đặt ở nhiều dòng thì cộng chung; số tối đa gợi ý cho một dòng là phần còn lại sau các dòng kia.
// SKU chưa có dòng tồn ở kho (chưa từng nhập kho, S5-04) coi như tồn 0: hết hàng, gửi đơn bị chặn.
public class OrderStock {

    private final String warehouseName;
    private final List<Line> lines;

    public OrderStock(String warehouseName, List<Line> lines) {
        this.warehouseName = warehouseName;
        this.lines = List.copyOf(lines);
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public List<Line> getLines() {
        return lines;
    }

    // Tồn của một SKU ở một kho (stock_balances), theo đơn vị cơ sở
    public static final class Level {
        private final BigDecimal onHand;
        private final BigDecimal reserved;

        public Level(BigDecimal onHand, BigDecimal reserved) {
            this.onHand = onHand;
            this.reserved = reserved;
        }

        public BigDecimal getAvailable() {
            return onHand.subtract(reserved);
        }
    }

    // Dòng có lỗi giá/số lượng thì không kiểm tồn (checked = false)
    public static final class Line {
        private final boolean checked;
        private final BigDecimal available;
        private final BigDecimal maxQty;
        private final boolean shortage;
        private final String baseUnitName;
        private final String unitName;
        private final BigDecimal factor;

        private Line(boolean checked, BigDecimal available, BigDecimal maxQty, boolean shortage,
                     String baseUnitName, String unitName, BigDecimal factor) {
            this.checked = checked;
            this.available = available;
            this.maxQty = maxQty;
            this.shortage = shortage;
            this.baseUnitName = baseUnitName;
            this.unitName = unitName;
            this.factor = factor;
        }

        public static Line unchecked() {
            return new Line(false, null, null, false, null, null, null);
        }

        // room: phần tồn khả dụng còn lại cho dòng này sau các dòng khác cùng SKU (đơn vị cơ sở)
        public static Line of(BigDecimal available, BigDecimal room, boolean shortage, OrderProduct product,
                                   OrderProduct.Unit unit) {
            BigDecimal positive = room.max(BigDecimal.ZERO);
            // Đơn vị lớn (Thùng, Lốc...) gợi ý số nguyên; đơn vị cơ sở gợi ý đúng số còn
            BigDecimal maxQty = unit.isBase() ? positive
                    : positive.divide(unit.getFactor(), 0, RoundingMode.DOWN);
            OrderProduct.Unit baseUnit = product.getBaseUnit();
            return new Line(true, available, maxQty, shortage, baseUnit == null ? unit.getName() : baseUnit.getName(),
                    unit.getName(), unit.getFactor());
        }

        public boolean isChecked() {
            return checked;
        }

        // Kho không còn tồn khả dụng (chưa nhập kho, hoặc đã bị giữ chỗ hết cho đơn khác)
        public boolean isSoldOut() {
            return checked && available.signum() <= 0;
        }

        public boolean isShortage() {
            return shortage;
        }

        // vd "75 Lon (≈ 3 Thùng)" khi đang đặt theo Thùng
        public String getAvailableText() {
            if (!checked) {
                return null;
            }
            String text = qtyText(available) + " " + baseUnitName;
            BigDecimal inUnit = available.divide(factor, 0, RoundingMode.DOWN);
            // Chưa đủ 1 Thùng thì không ghi "≈ 0 Thùng"
            if (factor.compareTo(BigDecimal.ONE) != 0 && inUnit.signum() > 0) {
                text += " (≈ " + qtyText(inUnit) + " " + unitName + ")";
            }
            return text;
        }

        public BigDecimal getMaxQty() {
            return maxQty;
        }

        public String getMaxQtyText() {
            return maxQty == null ? null : qtyText(maxQty);
        }

        public String getMaxText() {
            return maxQty == null ? null : qtyText(maxQty) + " " + unitName;
        }

        // Câu báo khi gửi đơn vượt tồn
        public String getShortageMessage(String warehouseName) {
            String where = warehouseName == null ? "kho" : warehouseName;
            if (isSoldOut()) {
                return "Hết hàng: " + where + " không còn tồn khả dụng của mặt hàng này.";
            }
            if (maxQty.signum() == 0) {
                return "Vượt tồn khả dụng: " + where + " chỉ còn " + getAvailableText() + ", không đặt thêm được.";
            }
            return "Vượt tồn khả dụng: " + where + " còn " + getAvailableText() + ". Đặt tối đa " + getMaxText() + ".";
        }
    }

    // Số lượng hiển thị: số nguyên ngăn nghìn bằng dấu chấm (1.000), số lẻ dùng dấu phẩy thập phân như ô nhập (1,5)
    static String qtyText(BigDecimal qty) {
        BigDecimal value = qty.stripTrailingZeros();
        return value.scale() <= 0 ? MoneyUtil.format(value) : value.toPlainString().replace('.', ',');
    }
}
