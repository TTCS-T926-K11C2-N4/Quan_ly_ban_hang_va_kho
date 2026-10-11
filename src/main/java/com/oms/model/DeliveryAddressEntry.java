package com.oms.model;

// Một điểm giao hàng đầy đủ thông tin cho màn Điểm giao hàng (S3-04); used = đã có đơn hàng ghi điểm giao này
public class DeliveryAddressEntry {

    private final long id;
    private final long customerId;
    private final String label;
    private final String address;
    private final String receiverName;
    private final String receiverPhone;
    private final String routeNote;
    private final boolean defaultAddress;
    private final boolean active;
    private final boolean used;

    public DeliveryAddressEntry(long id, long customerId, String label, String address, String receiverName,
                                String receiverPhone, String routeNote, boolean defaultAddress, boolean active,
                                boolean used) {
        this.id = id;
        this.customerId = customerId;
        this.label = label;
        this.address = address;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.routeNote = routeNote;
        this.defaultAddress = defaultAddress;
        this.active = active;
        this.used = used;
    }

    public long getId() {
        return id;
    }

    public long getCustomerId() {
        return customerId;
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

    public String getRouteNote() {
        return routeNote;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isUsed() {
        return used;
    }
}
