package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

// Chuyến giao hàng (S6-06): gom nhiều phiếu xuất (mỗi phiếu một điểm giao) cho một xe, một tài xế, theo khu vực.
// Tài xế không phải user hệ thống nên lưu thẳng tên, số điện thoại, biển số xe.
public class DeliveryTrip {

    public static final String PLANNED = "PLANNED";
    public static final String RUNNING = "RUNNING";
    public static final String COMPLETED = "COMPLETED";

    private static final Map<String, String> LABELS = Map.of(PLANNED, "Đã lập", RUNNING, "Đang giao",
            COMPLETED, "Hoàn thành");
    private static final Map<String, String> TONES = Map.of(PLANNED, "neutral", RUNNING, "warning",
            COMPLETED, "success");

    private final long id;
    private final String tripNo;
    private final long warehouseId;
    private final String warehouseName;
    private final String regionName;
    private final String driverName;
    private final String driverPhone;
    private final String vehiclePlate;
    private final String status;
    private final LocalDateTime plannedDeparture;
    private final LocalDateTime expectedReturnAt;
    private final LocalDateTime departedAt;
    private final LocalDateTime completedAt;
    private final String note;
    private final int stopCount;
    private final int doneCount;
    private final List<Stop> stops;

    public DeliveryTrip(long id, String tripNo, long warehouseId, String warehouseName, String regionName,
                        String driverName, String driverPhone, String vehiclePlate, String status,
                        LocalDateTime plannedDeparture, LocalDateTime expectedReturnAt, LocalDateTime departedAt,
                        LocalDateTime completedAt, String note, int stopCount, int doneCount, List<Stop> stops) {
        this.id = id;
        this.tripNo = tripNo;
        this.warehouseId = warehouseId;
        this.warehouseName = warehouseName;
        this.regionName = regionName;
        this.driverName = driverName;
        this.driverPhone = driverPhone;
        this.vehiclePlate = vehiclePlate;
        this.status = status;
        this.plannedDeparture = plannedDeparture;
        this.expectedReturnAt = expectedReturnAt;
        this.departedAt = departedAt;
        this.completedAt = completedAt;
        this.note = note;
        this.stopCount = stopCount;
        this.doneCount = doneCount;
        this.stops = List.copyOf(stops);
    }

    public long getId() {
        return id;
    }

    public String getTripNo() {
        return tripNo;
    }

    public long getWarehouseId() {
        return warehouseId;
    }

    public String getWarehouseName() {
        return warehouseName;
    }

    public String getRegionName() {
        return regionName;
    }

    public String getDriverName() {
        return driverName;
    }

    public String getDriverPhone() {
        return driverPhone;
    }

    public String getVehiclePlate() {
        return vehiclePlate;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusLabel() {
        return LABELS.getOrDefault(status, status);
    }

    public String getStatusTone() {
        return TONES.getOrDefault(status, "neutral");
    }

    public boolean isPlanned() {
        return PLANNED.equals(status);
    }

    public boolean isRunning() {
        return RUNNING.equals(status);
    }

    public String getPlannedDepartureText() {
        return DateTimeUtil.formatDateTime(plannedDeparture);
    }

    public String getExpectedReturnAtText() {
        return DateTimeUtil.formatDateTime(expectedReturnAt);
    }

    public String getDepartedAtText() {
        return DateTimeUtil.formatDateTime(departedAt);
    }

    public String getCompletedAtText() {
        return DateTimeUtil.formatDateTime(completedAt);
    }

    public String getNote() {
        return note;
    }

    public int getStopCount() {
        return stopCount;
    }

    // Số điểm đã có kết quả (giao thành công hoặc không thành công)
    public int getDoneCount() {
        return doneCount;
    }

    public int getRemainingCount() {
        return stopCount - doneCount;
    }

    public List<Stop> getStops() {
        return stops;
    }

    // Chuyến đang giao mà đã quá giờ dự kiến về (S6-07)
    public boolean isOverdue() {
        return isOverdueAt(LocalDateTime.now(ZoneOffset.UTC));
    }

    boolean isOverdueAt(LocalDateTime nowUtc) {
        return RUNNING.equals(status) && expectedReturnAt != null && nowUtc.isAfter(expectedReturnAt);
    }

    // Tiền hàng của các điểm chưa có kết quả giao (đang nằm trên đường)
    public BigDecimal getOnRoadValue() {
        return stops.stream().filter(stop -> !stop.isDone()).map(Stop::getValue).reduce(BigDecimal.ZERO,
                BigDecimal::add);
    }

    // Các điểm giao không thành công: hàng vẫn thuộc trách nhiệm của chuyến cho tới khi chuyến hoàn thành (S6-07)
    public List<Stop> getFailedStops() {
        return stops.stream().filter(Stop::isFailed).toList();
    }

    public BigDecimal getFailedValue() {
        return getFailedStops().stream().map(Stop::getValue).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Một điểm giao của chuyến: phiếu xuất, đơn, đại lý, địa chỉ và người nhận
    public static class Stop {
        private final long noteId;
        private final String deliveryNo;
        private final String noteStatus;
        private final long orderId;
        private final String orderNo;
        private final String customerName;
        private final String addressLabel;
        private final String address;
        private final String receiverName;
        private final String receiverPhone;
        private final String routeNote;
        private final BigDecimal value;

        public Stop(long noteId, String deliveryNo, String noteStatus, long orderId, String orderNo,
                    String customerName, String addressLabel, String address, String receiverName,
                    String receiverPhone, String routeNote, BigDecimal value) {
            this.noteId = noteId;
            this.deliveryNo = deliveryNo;
            this.noteStatus = noteStatus;
            this.orderId = orderId;
            this.orderNo = orderNo;
            this.customerName = customerName;
            this.addressLabel = addressLabel;
            this.address = address;
            this.receiverName = receiverName;
            this.receiverPhone = receiverPhone;
            this.routeNote = routeNote;
            this.value = value;
        }

        public long getNoteId() {
            return noteId;
        }

        public String getDeliveryNo() {
            return deliveryNo;
        }

        public String getNoteStatus() {
            return noteStatus;
        }

        public String getNoteStatusLabel() {
            return DeliveryNoteStatus.label(noteStatus);
        }

        // Đã có kết quả giao (thành công hoặc không thành công)
        public boolean isDone() {
            return DeliveryNoteStatus.DELIVERED.equals(noteStatus) || DeliveryNoteStatus.FAILED.equals(noteStatus);
        }

        public boolean isFailed() {
            return DeliveryNoteStatus.FAILED.equals(noteStatus);
        }

        public long getOrderId() {
            return orderId;
        }

        public String getOrderNo() {
            return orderNo;
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getAddressLabel() {
            return addressLabel;
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

        public String getRouteNote() {
            return routeNote;
        }

        public BigDecimal getValue() {
            return value;
        }
    }
}
