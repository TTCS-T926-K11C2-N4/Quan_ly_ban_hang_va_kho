package com.oms.service;

import com.oms.model.PickListDetail;
import com.oms.model.PickListRow;
import com.oms.model.PickListStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PickListServiceTest {

    // Phiếu có hai dòng: 48 lon bia ở A-01 và 24 chai nước ở A-03
    private static PickListDetail pickList() {
        return new PickListDetail(1, "PSH-001", 1, "KHO-TT", "Kho trung tâm", PickListStatus.PICKING, null, "Lã Thế Đan",
                null, null, List.of(),
                List.of(new PickListDetail.Line(11, 2, "DH-1", "SP001", "Bia Hà Nội lon", "Lon", "L2610A", false, null,
                                "A-01", "Kệ bia lon", new BigDecimal("48.000"), null, null),
                        new PickListDetail.Line(12, 2, "DH-1", "SP011", "Nước suối", "Chai", "MAC-DINH", true, null,
                                "A-03", null, new BigDecimal("24.000"), new BigDecimal("24.000"), null)));
    }

    private static PickListService.LineInput picked(String qty, String reason) {
        return new PickListService.LineInput(true, qty, reason);
    }

    // S6-03 AC: đánh dấu đã soạn từng dòng, ghi số lượng thực soạn
    @Test
    void savingProgressAllowsUnpickedLines() {
        Map<Long, String> errors = PickListService.validate(pickList(), Map.of(11L, picked("48", null)), false);
        assertTrue(errors.isEmpty(), errors.toString());
    }

    @Test
    void completingRequiresEveryLinePicked() {
        Map<Long, String> errors = PickListService.validate(pickList(), Map.of(11L, picked("48", null)), true);
        assertEquals(List.of(12L), List.copyOf(errors.keySet()));
    }

    // S6-03 AC: soạn thiếu bắt buộc nhập lý do
    @Test
    void shortPickNeedsReasonFromList() {
        assertEquals("Soạn thiếu 8, bắt buộc chọn lý do.",
                PickListService.validate(pickList(), Map.of(11L, picked("40", null)), false).get(11L));
        assertTrue(PickListService.validate(pickList(), Map.of(11L, picked("40", "Lý do tự gõ")), false)
                .containsKey(11L));
        assertTrue(PickListService.validate(pickList(), Map.of(11L, picked("40", "Hết hàng tại vị trí")), false)
                .isEmpty());
    }

    @Test
    void pickedQuantityCannotExceedRequiredOrBeInvalid() {
        assertEquals("SL lấy không vượt SL cần (48).",
                PickListService.validate(pickList(), Map.of(11L, picked("50", null)), false).get(11L));
        assertTrue(PickListService.validate(pickList(), Map.of(11L, picked("-1", null)), false).containsKey(11L));
        assertTrue(PickListService.validate(pickList(), Map.of(11L, picked("4,5678", null)), false).containsKey(11L));
    }

    @Test
    void parseQtyAcceptsCommaDecimals() {
        assertEquals(new BigDecimal("12.5"), PickListService.parseQty("12,5"));
        assertEquals(new BigDecimal("0"), PickListService.parseQty("0"));
        assertNull(PickListService.parseQty("abc"));
        assertNull(PickListService.parseQty(null));
    }

    @Test
    void progressCountsPickedAndShortLines() {
        PickListDetail detail = new PickListDetail(1, "PSH-001", 1, "KHO-TT", "Kho trung tâm", PickListStatus.PICKING,
                null, null, null, null, List.of(),
                List.of(new PickListDetail.Line(11, 2, "DH-1", "SP001", "Bia", "Lon", "L1", false, null, null, null,
                                new BigDecimal("48"), new BigDecimal("40"), "Hết hàng tại vị trí"),
                        new PickListDetail.Line(12, 2, "DH-1", "SP011", "Nước", "Chai", "MAC-DINH", true, null, null,
                                null, new BigDecimal("24"), null, null)));
        assertEquals(1, detail.getPickedLines());
        assertEquals(1, detail.getShortLines());
        assertEquals(50, detail.getProgressPercent());
    }

    // Phiếu soạn xong và mọi đơn đã xác nhận xuất kho (S6-05) thì hiện "Đã xuất kho"
    @Test
    void doneListShowsIssuedWhenAllOrdersIssued() {
        PickListRow waiting = new PickListRow(1, "PSH-001", "DH-1, DH-2", "Kho trung tâm", null, PickListStatus.DONE,
                2, 1, null);
        PickListRow issued = new PickListRow(1, "PSH-001", "DH-1, DH-2", "Kho trung tâm", null, PickListStatus.DONE,
                2, 2, null);
        assertEquals("Đã soạn", waiting.getStatusLabel());
        assertTrue(waiting.isAwaitingIssue());
        assertEquals("Đã xuất kho", issued.getStatusLabel());
    }
}
