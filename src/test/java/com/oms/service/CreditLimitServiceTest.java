package com.oms.service;

import com.oms.model.CreditLimitForm;
import com.oms.model.Customer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreditLimitServiceTest {

    private final CreditLimitService service = new CreditLimitService();

    private static Customer customer(String limit, int days) {
        return new Customer(1, "DL00125", "Đại lý Minh Anh", null, null, null, null, 1, null, new BigDecimal(limit),
                days, false, null, Customer.ACTIVE, 3);
    }

    // S3-05 AC1 + AC2: hạn mức tiền, số ngày nợ và lý do đều bắt buộc
    @Test
    void requiresLimitDaysAndReason() {
        Map<String, String> errors = service.validate(new CreditLimitForm(null, null, null, "3"),
                customer("0.00", 0));
        assertEquals(Map.of(
                "creditLimit", "Vui lòng nhập hạn mức tiền tối đa.",
                "maxDebtDays", "Vui lòng nhập số ngày nợ tối đa.",
                "reason", "Vui lòng nhập lý do thay đổi hạn mức."), errors);
    }

    @Test
    void acceptsThousandSeparatorsOrPlainDigits() {
        assertEquals(new BigDecimal("100000000"), CreditLimitService.parseMoney("100.000.000"));
        assertEquals(new BigDecimal("100000000"), CreditLimitService.parseMoney("100000000"));
        for (String bad : new String[] {"-1", "100,5", "1.00.000", "10.5", "abc", "1 000"}) {
            assertNull(CreditLimitService.parseMoney(bad), bad);
        }
    }

    @Test
    void rejectsOutOfRangeValues() {
        Map<String, String> errors = service.validate(
                new CreditLimitForm("12345678901234567", "366", "Tăng hạn mức", "3"), customer("0.00", 0));
        assertEquals("Hạn mức tiền tối đa 16 chữ số.", errors.get("creditLimit"));
        assertEquals("Số ngày nợ tối đa là số nguyên từ 0 đến 365.", errors.get("maxDebtDays"));
    }

    // Không đổi gì thì không ghi lịch sử và nhật ký
    @Test
    void reportsUnchangedValues() {
        Map<String, String> errors = service.validate(new CreditLimitForm("100.000.000", "30", "Kiểm tra", "3"),
                customer("100000000.00", 30));
        assertEquals(Map.of("form", "Hạn mức tiền và số ngày nợ chưa thay đổi so với hiện tại."), errors);
    }

    @Test
    void validChangePasses() {
        assertTrue(service.validate(new CreditLimitForm("150.000.000", "45", "Đại lý trả đúng hạn", "3"),
                customer("100000000.00", 30)).isEmpty());
    }

    @Test
    void formShowsCurrentLimitWithThousandSeparators() {
        CreditLimitForm form = CreditLimitForm.of(customer("100000000.00", 30));
        assertEquals("100.000.000", form.getCreditLimit());
        assertEquals("30", form.getMaxDebtDays());
        assertEquals("3", form.getVersion());
    }
}
