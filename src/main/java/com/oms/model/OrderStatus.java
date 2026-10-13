package com.oms.model;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

// Vòng đời đơn hàng (S4-06): Nháp → Chờ duyệt → Đã duyệt → Đang soạn → Đã xuất → Đã giao → Đóng,
// nhánh rẽ Bị từ chối (từ Chờ duyệt) và Đã huỷ. Mã trạng thái khớp ràng buộc CHECK của sales_orders.status.
public final class OrderStatus {

    public static final String DRAFT = "DRAFT";
    public static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String APPROVED = "APPROVED";
    public static final String PICKING = "PICKING";
    public static final String SHIPPED = "SHIPPED";
    public static final String DELIVERED = "DELIVERED";
    public static final String CLOSED = "CLOSED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";

    // Các bước chính theo thứ tự hiển thị trên thanh tiến trình
    public static final List<String> STEPS = List.of(DRAFT, PENDING_APPROVAL, APPROVED, PICKING, SHIPPED, DELIVERED,
            CLOSED);

    // Huỷ được khi đơn đã gửi nhưng chưa xuất kho; đã xuất thì phải đi đường trả hàng (AC3)
    public static final Set<String> CANCELLABLE = Set.of(PENDING_APPROVAL, APPROVED, PICKING);

    private static final Map<String, String> LABELS = Map.of(
            DRAFT, "Nháp", PENDING_APPROVAL, "Chờ duyệt", APPROVED, "Đã duyệt", PICKING, "Đang soạn hàng",
            SHIPPED, "Đã xuất kho", DELIVERED, "Đã giao", CLOSED, "Đóng", REJECTED, "Bị từ chối",
            CANCELLED, "Đã huỷ");

    private static final Map<String, String> DESCRIPTIONS = Map.of(
            DRAFT, "Đơn đang soạn, chưa gửi duyệt", PENDING_APPROVAL, "Đã gửi, chờ quản lý duyệt",
            APPROVED, "Đã duyệt, chờ kho soạn hàng", PICKING, "Kho đang soạn hàng",
            SHIPPED, "Đã xuất kho, đang giao", DELIVERED, "Đã giao cho đại lý", CLOSED, "Đơn hoàn tất",
            REJECTED, "Quản lý không duyệt đơn", CANCELLED, "Đơn đã bị huỷ");

    private OrderStatus() {
    }

    public static String label(String status) {
        return LABELS.getOrDefault(status, status);
    }

    public static String description(String status) {
        return DESCRIPTIONS.getOrDefault(status, "");
    }

    public static boolean isCancellable(String status) {
        return CANCELLABLE.contains(status);
    }

    // Nhánh rẽ: đơn dừng lại, không đi tiếp các bước sau
    public static boolean isTerminalBranch(String status) {
        return REJECTED.equals(status) || CANCELLED.equals(status);
    }

    // Tên class màu của nhãn trạng thái (order-status--*)
    public static String tone(String status) {
        return status == null ? "" : status.toLowerCase().replace('_', '-');
    }

    // Bảng "Ý nghĩa các trạng thái": các bước chính theo thứ tự rồi tới hai nhánh rẽ
    public static List<Legend> legend() {
        return Stream.concat(STEPS.stream(), Stream.of(REJECTED, CANCELLED))
                .map(code -> new Legend(label(code), description(code), tone(code))).toList();
    }

    public record Legend(String label, String description, String tone) {
    }
}
