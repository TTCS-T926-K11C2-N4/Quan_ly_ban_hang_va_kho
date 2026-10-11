package com.oms.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

// Hiện tiền VNĐ kiểu Việt Nam: 2500000 -> "2.500.000"
public final class MoneyUtil {

    private MoneyUtil() {
    }

    public static String format(BigDecimal amount) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat format = new DecimalFormat("#,##0", symbols);
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format.format(amount);
    }
}
