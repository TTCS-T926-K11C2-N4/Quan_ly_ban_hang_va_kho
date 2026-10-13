package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

// Công nợ của đại lý trên màn tạo đơn (S4-02). Đã dùng = nợ trên sổ công nợ (customer_balances) + các đơn đã gửi
// chưa xuất hoá đơn, để hai đơn gửi liên tiếp không cùng lọt hạn mức. Hạn mức 0 là chưa được cho nợ: mọi đơn đều vượt.
// Quá hạn: còn hoá đơn chưa thu hết mà hạn trả (ngày phát hành + số ngày nợ chốt lúc phát hành) đã qua.
public class CustomerCredit {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BigDecimal creditLimit;
    private final int maxDebtDays;
    private final BigDecimal ledgerBalance;
    private final BigDecimal openOrders;
    private final BigDecimal overdueAmount;
    private final LocalDate oldestDueDate;

    public CustomerCredit(BigDecimal creditLimit, int maxDebtDays, BigDecimal ledgerBalance, BigDecimal openOrders,
                          BigDecimal overdueAmount, LocalDate oldestDueDate) {
        this.creditLimit = creditLimit;
        this.maxDebtDays = maxDebtDays;
        this.ledgerBalance = ledgerBalance;
        this.openOrders = openOrders;
        this.overdueAmount = overdueAmount;
        this.oldestDueDate = oldestDueDate;
    }

    public boolean isLimitGranted() {
        return creditLimit.signum() > 0;
    }

    public BigDecimal getUsed() {
        return ledgerBalance.add(openOrders);
    }

    // Âm khi đã vượt hạn mức
    public BigDecimal getAvailable() {
        return creditLimit.subtract(getUsed());
    }

    public BigDecimal getAvailableAfter(BigDecimal orderTotal) {
        return getAvailable().subtract(orderTotal);
    }

    // AC2: tổng đơn cộng công nợ hiện tại vượt hạn mức thì đơn cần duyệt
    public boolean exceeds(BigDecimal orderTotal) {
        return getAvailableAfter(orderTotal).signum() < 0;
    }

    public BigDecimal getExceededBy(BigDecimal orderTotal) {
        return getAvailableAfter(orderTotal).negate().max(BigDecimal.ZERO);
    }

    // Phần trăm hạn mức đã dùng cho thanh tiến độ, tối đa 100
    public int getUsedPercent() {
        if (!isLimitGranted()) {
            return getUsed().signum() > 0 ? 100 : 0;
        }
        return getUsed().multiply(HUNDRED).divide(creditLimit, 0, RoundingMode.DOWN).min(HUNDRED)
                .intValue();
    }

    // AC3: có khoản quá hạn thì chặn tạo đơn
    public boolean isOverdue() {
        return overdueAmount.signum() > 0;
    }

    public String getOldestDueDateText() {
        return oldestDueDate == null ? null : DateTimeUtil.formatDay(oldestDueDate);
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public int getMaxDebtDays() {
        return maxDebtDays;
    }

    public BigDecimal getLedgerBalance() {
        return ledgerBalance;
    }

    public BigDecimal getOpenOrders() {
        return openOrders;
    }

    public BigDecimal getOverdueAmount() {
        return overdueAmount;
    }
}
