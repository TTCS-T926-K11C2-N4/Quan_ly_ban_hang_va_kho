package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Chi tiết giữ chỗ của một đơn (S5-06): thông tin đơn và trạng thái giữ chỗ từng dòng hàng
public class ReservationOrderDetail {

    private final long orderId;
    private final String orderNo;
    private final String orderStatus;
    private final LocalDateTime approvedAt;
    private final String customerName;
    private final String customerCode;
    private final List<Line> lines;

    public ReservationOrderDetail(long orderId, String orderNo, String orderStatus, LocalDateTime approvedAt,
                                  String customerName, String customerCode, List<Line> lines) {
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.orderStatus = orderStatus;
        this.approvedAt = approvedAt;
        this.customerName = customerName;
        this.customerCode = customerCode;
        this.lines = List.copyOf(lines);
    }

    public long getOrderId() {
        return orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public String getOrderStatus() {
        return orderStatus;
    }

    public String getOrderStatusLabel() {
        return OrderStatus.label(orderStatus);
    }

    public boolean isOrderLive() {
        return !OrderStatus.isTerminalBranch(orderStatus);
    }

    public String getApprovedAtText() {
        return approvedAt == null ? "—" : DateTimeUtil.formatDate(approvedAt);
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerCode() {
        return customerCode;
    }

    public List<Line> getLines() {
        return lines;
    }

    public long getHeldLineCount() {
        return lines.stream().filter(line -> line.getState() == ReservationState.HELD).count();
    }

    // Tổng số lượng đang giữ chỗ (đơn vị cơ sở)
    public BigDecimal getReservedTotal() {
        return lines.stream().filter(line -> line.getState() == ReservationState.HELD).map(Line::getReservedQty)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public ReservationState getState() {
        int expired = (int) lines.stream().filter(line -> line.getState() == ReservationState.EXPIRED).count();
        int released = (int) lines.stream().filter(line -> line.getState() == ReservationState.EXPIRED
                || line.getState() == ReservationState.RELEASED).count();
        int fulfilled = (int) lines.stream().filter(line -> line.getState() == ReservationState.FULFILLED).count();
        return ReservationState.of(lines.size(), (int) getHeldLineCount(), expired, released, fulfilled);
    }

    // Một dòng hàng: số lượng đặt và số lượng giữ chỗ theo đơn vị cơ sở, tại kho phục vụ của đơn
    public static class Line {
        private final String sku;
        private final String productName;
        private final String baseUnitName;
        private final String warehouseCode;
        private final String warehouseName;
        private final BigDecimal orderedQty;
        private final BigDecimal reservedQty;
        private final ReservationState state;
        private final LocalDateTime expiresAt;

        public Line(String sku, String productName, String baseUnitName, String warehouseCode, String warehouseName,
                    BigDecimal orderedQty, BigDecimal reservedQty, ReservationState state, LocalDateTime expiresAt) {
            this.sku = sku;
            this.productName = productName;
            this.baseUnitName = baseUnitName;
            this.warehouseCode = warehouseCode;
            this.warehouseName = warehouseName;
            this.orderedQty = orderedQty;
            this.reservedQty = reservedQty;
            this.state = state;
            this.expiresAt = expiresAt;
        }

        public String getSku() {
            return sku;
        }

        public String getProductName() {
            return productName;
        }

        public String getBaseUnitName() {
            return baseUnitName;
        }

        public String getWarehouseCode() {
            return warehouseCode;
        }

        public String getWarehouseName() {
            return warehouseName;
        }

        public BigDecimal getOrderedQty() {
            return orderedQty;
        }

        // 0 khi dòng chưa giữ chỗ
        public BigDecimal getReservedQty() {
            return reservedQty;
        }

        public ReservationState getState() {
            return state;
        }

        // Hạn giữ chỗ (giờ Việt Nam); null khi dòng chưa giữ chỗ
        public String getExpiresAtText() {
            return DateTimeUtil.formatDateTime(expiresAt);
        }
    }
}
