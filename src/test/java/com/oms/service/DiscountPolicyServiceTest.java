package com.oms.service;

import com.oms.model.DiscountPolicyForm;
import com.oms.model.DiscountPolicyRow;
import com.oms.model.DiscountPolicyStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DiscountPolicyServiceTest {

    private static DiscountPolicyForm form(List<DiscountPolicyForm.TierLine> tiers) {
        return new DiscountPolicyForm(null, "CK", null, "PRODUCT", "SP001", null, "PERCENT", "2026-10-01", null, true,
                tiers);
    }

    private static DiscountPolicyForm.TierLine tier(String qty, String value) {
        return new DiscountPolicyForm.TierLine(qty, value);
    }

    // S3-01 AC1/AC2: bậc theo số lượng, lưu theo số lượng tăng dần; % cho dấu phẩy thập phân, tiền cho dấu chấm nghìn
    @Test
    void parsesTiersSortedByQuantity() {
        Map<String, String> errors = new LinkedHashMap<>();
        List<DiscountPolicyRow.Tier> tiers = DiscountPolicyService.parseTiers(
                form(List.of(tier("96", "4"), tier("48", "2,5"), tier(null, null))), true, errors);
        assertTrue(errors.isEmpty());
        assertEquals(new BigDecimal("48"), tiers.get(0).minQtyBase());
        assertEquals(new BigDecimal("2.5"), tiers.get(0).value());
        assertEquals(new BigDecimal("96"), tiers.get(1).minQtyBase());

        List<DiscountPolicyRow.Tier> amount = DiscountPolicyService.parseTiers(form(List.of(tier("24", "1.500"))),
                false, errors);
        assertEquals(new BigDecimal("1500"), amount.get(0).value());
    }

    @Test
    void rejectsBadTiers() {
        Map<String, String> errors = new LinkedHashMap<>();
        DiscountPolicyService.parseTiers(form(List.of(tier(null, null))), true, errors);
        assertEquals("Khai báo ít nhất một bậc chiết khấu.", errors.get("tiers"));

        errors.clear();
        DiscountPolicyService.parseTiers(form(List.of(tier("24", "2"), tier("24", "3"))), true, errors);
        assertEquals("Bậc 2: trùng số lượng tối thiểu với một bậc khác.", errors.get("tiers"));

        errors.clear();
        DiscountPolicyService.parseTiers(form(List.of(tier("24", "120"))), true, errors);
        assertEquals("Bậc 1: phần trăm chiết khấu không quá 100.", errors.get("tiers"));

        errors.clear();
        DiscountPolicyService.parseTiers(form(List.of(tier("0", "5"))), true, errors);
        assertEquals("Bậc 1: số lượng tối thiểu phải là số lớn hơn 0.", errors.get("tiers"));
    }

    @Test
    void statusCombinesSwitchAndValidity() {
        LocalDate today = LocalDate.of(2026, 10, 8);
        assertEquals(DiscountPolicyStatus.INACTIVE,
                DiscountPolicyStatus.of(false, LocalDate.of(2026, 1, 1), null, today));
        assertEquals(DiscountPolicyStatus.ACTIVE, DiscountPolicyStatus.of(true, LocalDate.of(2026, 1, 1), null, today));
        assertEquals(DiscountPolicyStatus.UPCOMING,
                DiscountPolicyStatus.of(true, LocalDate.of(2026, 11, 1), null, today));
        assertEquals(DiscountPolicyStatus.EXPIRED,
                DiscountPolicyStatus.of(true, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 9, 30), today));
    }
}
