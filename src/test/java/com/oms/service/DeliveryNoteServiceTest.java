package com.oms.service;

import com.oms.dao.DeliveryNoteDao;
import com.oms.model.DeliveryIssueDraft;
import com.oms.model.DeliveryNoteStatus;
import com.oms.model.PickListStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryNoteServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 15);

    // Đơn có 2 dòng: bia cần 48 (soạn được 40, thiếu do hết hàng), nước cần 24 (soạn đủ)
    private static DeliveryIssueDraft draft() {
        return new DeliveryIssueDraft(1, "PSH-001", PickListStatus.DONE, 1, "Kho trung tâm", "Lã Thế Đan", 2, "DH-1",
                "PICKING", "Đại lý Hoàng Long", TODAY, null,
                List.of(new DeliveryIssueDraft.Line(3, 18, "SP001", "Bia Hà Nội lon", "Lon", new BigDecimal("48"),
                                new BigDecimal("40"), "Hết hàng tại vị trí"),
                        new DeliveryIssueDraft.Line(4, 6, "SP011", "Nước suối", "Chai", new BigDecimal("24"),
                                new BigDecimal("24"), null)));
    }

    private static DeliveryNoteService.IssueForm form(String beer, String reason, String action, String date) {
        return new DeliveryNoteService.IssueForm(Map.of(3L, new DeliveryNoteService.LineInput(beer, reason),
                4L, new DeliveryNoteService.LineInput("24", null)), action, date, "09:30", null);
    }

    // Mở form lần đầu: thực xuất = số đã soạn, lý do lấy từ lúc soạn, mặc định giao bổ sung ngày mai
    @Test
    void defaultFormUsesPickedQuantities() {
        DeliveryNoteService.IssueForm form = DeliveryNoteService.defaultForm(draft(), TODAY);
        assertEquals("40", form.lines().get(3L).qty());
        assertEquals("Hết hàng tại vị trí", form.lines().get(3L).reason());
        assertEquals(DeliveryNoteStatus.BACKORDER, form.shortageAction());
        assertEquals("2026-10-16", form.backorderDate());
        assertTrue(DeliveryNoteService.validate(draft(), form, TODAY).isEmpty());
    }

    // S6-05: dòng xuất thiếu bắt buộc chọn lý do trước khi xác nhận
    @Test
    void shortLineNeedsReason() {
        Map<String, String> errors = DeliveryNoteService.validate(draft(), form("40", null, "CLOSE", null), TODAY);
        assertEquals("Dòng SP001 thiếu 8, bắt buộc chọn lý do trước khi xác nhận.", errors.get("line_3"));
    }

    @Test
    void issuedCannotExceedPicked() {
        Map<String, String> errors = DeliveryNoteService.validate(draft(), form("45", null, null, null), TODAY);
        assertEquals("Thực xuất không vượt số đã soạn (40).", errors.get("line_3"));
    }

    // Xuất thiếu phải chọn cách xử lý; giao bổ sung cần ngày không trước hôm nay
    @Test
    void shortageNeedsHandlingAndBackorderDate() {
        assertTrue(DeliveryNoteService.validate(draft(), form("40", "Hết hàng tại vị trí", null, null), TODAY)
                .containsKey("shortageAction"));
        assertTrue(DeliveryNoteService.validate(draft(), form("40", "Hết hàng tại vị trí", "BACKORDER", null), TODAY)
                .containsKey("backorderDate"));
        assertTrue(DeliveryNoteService.validate(draft(), form("40", "Hết hàng tại vị trí", "BACKORDER", "2026-10-14"),
                TODAY).containsKey("backorderDate"));
        assertTrue(DeliveryNoteService.validate(draft(), form("40", "Hết hàng tại vị trí", "CLOSE", null), TODAY)
                .isEmpty());
    }

    @Test
    void issuingNothingIsRejected() {
        DeliveryNoteService.IssueForm form = new DeliveryNoteService.IssueForm(
                Map.of(3L, new DeliveryNoteService.LineInput("0", "Hết hàng tại vị trí"),
                        4L, new DeliveryNoteService.LineInput("0", "Hết hàng tại vị trí")), "CLOSE", null, null, null);
        assertTrue(DeliveryNoteService.validate(draft(), form, TODAY).containsKey("lines"));
    }

    // Số thực xuất chia vào các lô đã soạn theo thứ tự hạn sử dụng (lô đầu trước)
    @Test
    void allocateTakesPickedLotsInOrder() throws Exception {
        List<DeliveryNoteDao.PickedLot> lots = List.of(
                new DeliveryNoteDao.PickedLot(3, 18, 33, "L2610A", new BigDecimal("30")),
                new DeliveryNoteDao.PickedLot(3, 18, 7, "MAC-DINH", new BigDecimal("10")));
        Map<Long, List<DeliveryNoteDao.PickedLot>> result = DeliveryNoteService.allocate(lots,
                Map.of(3L, new BigDecimal("35")));
        assertEquals(2, result.get(3L).size());
        assertEquals(new BigDecimal("30"), result.get(3L).get(0).qtyPicked());
        assertEquals(new BigDecimal("5"), result.get(3L).get(1).qtyPicked());
        assertThrows(DeliveryNoteService.DeliveryException.class,
                () -> DeliveryNoteService.allocate(lots, Map.of(3L, new BigDecimal("41"))));
    }

    // Lịch giao bổ sung nhập theo giờ Việt Nam, lưu UTC
    @Test
    void backorderTimeIsStoredInUtc() {
        assertEquals(LocalDateTime.of(2026, 10, 17, 2, 30), DeliveryNoteService.toUtc(LocalDate.of(2026, 10, 17), "09:30"));
        assertEquals(LocalDateTime.of(2026, 10, 17, 1, 0), DeliveryNoteService.toUtc(LocalDate.of(2026, 10, 17), null));
    }
}
