package com.oms.model;

import com.oms.util.MoneyUtil;

// Dữ liệu form "Sửa hạn mức công nợ" (S3-05), giữ nguyên chuỗi người dùng gõ để hiện lại khi có lỗi.
// version: version của đại lý lúc mở form, để phát hiện người khác vừa sửa (khoá lạc quan).
public class CreditLimitForm {

    private final String creditLimit;
    private final String maxDebtDays;
    private final String reason;
    private final String version;

    public CreditLimitForm(String creditLimit, String maxDebtDays, String reason, String version) {
        this.creditLimit = creditLimit;
        this.maxDebtDays = maxDebtDays;
        this.reason = reason;
        this.version = version;
    }

    public static CreditLimitForm of(Customer customer) {
        // 100000000 -> "100.000.000" như cách gõ trên form
        return new CreditLimitForm(MoneyUtil.format(customer.getCreditLimit()),
                String.valueOf(customer.getMaxDebtDays()), null, String.valueOf(customer.getVersion()));
    }

    public String getCreditLimit() {
        return creditLimit;
    }

    public String getMaxDebtDays() {
        return maxDebtDays;
    }

    public String getReason() {
        return reason;
    }

    public String getVersion() {
        return version;
    }
}
