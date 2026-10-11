package com.oms.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CustomerListStatusTest {

    // Khoá giao dịch hiện trước trạng thái, giống thẻ thông tin ở trang chi tiết đại lý
    @Test
    void blockedTakesPriorityOverActiveFlag() {
        assertEquals(CustomerListStatus.BLOCKED, CustomerListStatus.of(true, true));
        assertEquals(CustomerListStatus.BLOCKED, CustomerListStatus.of(true, false));
        assertEquals(CustomerListStatus.ACTIVE, CustomerListStatus.of(false, true));
        assertEquals(CustomerListStatus.INACTIVE, CustomerListStatus.of(false, false));
    }

    @Test
    void unknownFilterCodeMeansNoFilter() {
        assertEquals(CustomerListStatus.INACTIVE, CustomerListStatus.fromCode("INACTIVE"));
        assertNull(CustomerListStatus.fromCode("inactive"));
        assertNull(CustomerListStatus.fromCode(null));
    }
}
