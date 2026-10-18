package com.oms.service;

import com.oms.model.DeliveryNoteStatus;
import com.oms.model.DeliveryPoint;
import com.oms.model.DeliveryStop;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeliveryConfirmServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 15, 15, 0);

    private static DeliveryStop stop(long id, String status) {
        return new DeliveryStop(id, "PX-00" + id, status, 1, id, "DH-" + id, null, null, BigDecimal.TEN, null, null,
                null, null);
    }

    private static DeliveryPoint point(DeliveryStop... stops) {
        DeliveryPoint point = new DeliveryPoint(3, "Đại lý Hoàng Long", "DL00126", "Cửa hàng chính",
                "12 Hoàng Văn Thụ", "Lê Hoàng Long", "0912 456 789", null);
        point.getStops().addAll(List.of(stops));
        return point;
    }

    // S6-08 AC: giao thành công phải có người nhận, thời điểm giao và ảnh biên bản có chữ ký
    @Test
    void successNeedsRecipientTimeAndProof() {
        DeliveryConfirmService.SuccessForm form = new DeliveryConfirmService.SuccessForm("Lê Hoàng Long",
                "2026-10-15T14:30", null);
        assertTrue(DeliveryConfirmService.validateSuccess(form, 1, NOW).isEmpty());
        assertTrue(DeliveryConfirmService.validateSuccess(form, 0, NOW).containsKey("proofs"));
        assertTrue(DeliveryConfirmService.validateSuccess(form, 6, NOW).containsKey("proofs"));
        Map<String, String> errors = DeliveryConfirmService.validateSuccess(
                new DeliveryConfirmService.SuccessForm(null, "2026-10-15T16:00", null), 1, NOW);
        assertTrue(errors.containsKey("recipientName"));
        assertEquals("Thời điểm giao không được sau hiện tại.", errors.get("deliveredAt"));
    }

    // S6-08 AC: giao không thành công phải chọn lý do; lý do khác thì ghi rõ
    @Test
    void failureNeedsReasonFromList() {
        assertTrue(DeliveryConfirmService.validateFailure(new DeliveryConfirmService.FailureForm(null, null))
                .containsKey("reason"));
        assertTrue(DeliveryConfirmService.validateFailure(
                new DeliveryConfirmService.FailureForm("Khách hàng từ chối nhận hàng", null)).isEmpty());
        assertTrue(DeliveryConfirmService.validateFailure(
                new DeliveryConfirmService.FailureForm(DeliveryConfirmService.OTHER_REASON, null)).containsKey("remark"));
    }

    @Test
    void detectsOnlyJpegAndPng() {
        assertEquals("image/jpeg", DeliveryConfirmService.detectImageType(new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0}));
        assertEquals("image/png", DeliveryConfirmService.detectImageType(
                new byte[] {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A}));
        assertNull(DeliveryConfirmService.detectImageType("GIF89a".getBytes()));
        assertNull(DeliveryConfirmService.detectImageType(new byte[0]));
    }

    // Phạm vi hành động: chỉ phiếu còn chờ giao; không chọn gì thì lấy mọi phiếu chờ giao của điểm
    @Test
    void scopeTakesOnlyPendingStops() {
        DeliveryPoint point = point(stop(1, DeliveryNoteStatus.ISSUED), stop(2, DeliveryNoteStatus.DELIVERED),
                stop(3, DeliveryNoteStatus.ISSUED));
        assertEquals(List.of(1L, 3L), DeliveryConfirmService.selectStops(point, List.of()).stream()
                .map(DeliveryStop::getNoteId).toList());
        assertEquals(List.of(3L), DeliveryConfirmService.selectStops(point, List.of(2L, 3L)).stream()
                .map(DeliveryStop::getNoteId).toList());
    }

    @Test
    void pointStatusFollowsStops() {
        assertEquals("Chờ giao", point(stop(1, DeliveryNoteStatus.ISSUED)).getStatusLabel());
        assertEquals("Đang giao", point(stop(1, DeliveryNoteStatus.ISSUED), stop(2, DeliveryNoteStatus.DELIVERED))
                .getStatusLabel());
        assertEquals("Đã giao", point(stop(1, DeliveryNoteStatus.DELIVERED)).getStatusLabel());
        assertEquals("Không thành công", point(stop(1, DeliveryNoteStatus.FAILED)).getStatusLabel());
        assertEquals("Đã xử lý", point(stop(1, DeliveryNoteStatus.FAILED), stop(2, DeliveryNoteStatus.DELIVERED))
                .getStatusLabel());
        assertEquals("0912456789", point().getPhoneLink());
        assertEquals(new BigDecimal("20"), point(stop(1, DeliveryNoteStatus.ISSUED), stop(2, DeliveryNoteStatus.ISSUED))
                .getTotalValue());
    }
}
