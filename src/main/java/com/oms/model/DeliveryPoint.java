package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

// Điểm giao hàng trong ngày (S6-08): một điểm giao của đại lý và các phiếu xuất cần giao tới đó
public class DeliveryPoint {

    public static final String PENDING = "PENDING";
    public static final String DELIVERED = "DELIVERED";
    public static final String FAILED = "FAILED";
    public static final String PARTIAL = "PARTIAL";

    private final long addressId;
    private final String customerName;
    private final String customerCode;
    private final String label;
    private final String address;
    private final String receiverName;
    private final String receiverPhone;
    private final String routeNote;
    private final List<DeliveryStop> stops = new ArrayList<>();

    public DeliveryPoint(long addressId, String customerName, String customerCode, String label, String address,
                         String receiverName, String receiverPhone, String routeNote) {
        this.addressId = addressId;
        this.customerName = customerName;
        this.customerCode = customerCode;
        this.label = label;
        this.address = address;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.routeNote = routeNote;
    }

    public long getAddressId() {
        return addressId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public String getLabel() {
        return label;
    }

    public String getAddress() {
        return address;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    // Số gọi được (bỏ khoảng trắng, dấu chấm) cho liên kết tel:
    public String getPhoneLink() {
        return receiverPhone == null ? null : receiverPhone.replaceAll("[^0-9+]", "");
    }

    public String getRouteNote() {
        return routeNote;
    }

    public List<DeliveryStop> getStops() {
        return stops;
    }

    public List<DeliveryStop> getPendingStops() {
        return stops.stream().filter(DeliveryStop::isPending).toList();
    }

    public int getOrderCount() {
        return stops.size();
    }

    public int getPendingCount() {
        return (int) stops.stream().filter(DeliveryStop::isPending).count();
    }

    public int getDeliveredCount() {
        return (int) stops.stream().filter(DeliveryStop::isDelivered).count();
    }

    public int getFailedCount() {
        return (int) stops.stream().filter(DeliveryStop::isFailed).count();
    }

    // Còn phiếu chờ giao: Chờ giao; xử lý xong: Đã giao / Không thành công, lẫn cả hai thì Đã xử lý
    public String getStatus() {
        if (getPendingCount() > 0) {
            return getPendingCount() == stops.size() ? PENDING : PARTIAL;
        }
        if (getFailedCount() == 0) {
            return DELIVERED;
        }
        return getDeliveredCount() == 0 ? FAILED : PARTIAL;
    }

    public String getStatusLabel() {
        return switch (getStatus()) {
            case PENDING -> "Chờ giao";
            case DELIVERED -> "Đã giao";
            case FAILED -> "Không thành công";
            default -> getPendingCount() > 0 ? "Đang giao" : "Đã xử lý";
        };
    }

    public String getTone() {
        return getStatus().toLowerCase();
    }

    public BigDecimal getTotalValue() {
        return stops.stream().map(DeliveryStop::getValue).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getOrderNos() {
        return stops.stream().map(DeliveryStop::getOrderNo).distinct().collect(Collectors.joining(", "));
    }

    // Lịch giao: ngày hẹn giao sớm nhất của các đơn
    public LocalDate getScheduledDate() {
        return stops.stream().map(DeliveryStop::getRequestedDate).filter(Objects::nonNull).min(LocalDate::compareTo)
                .orElse(null);
    }

    public String getScheduledDateText() {
        LocalDate date = getScheduledDate();
        return date == null ? "Chưa hẹn" : DateTimeUtil.formatDay(date);
    }

    public DeliveryStop findStop(long noteId) {
        return stops.stream().filter(stop -> stop.getNoteId() == noteId).findFirst().orElse(null);
    }
}
