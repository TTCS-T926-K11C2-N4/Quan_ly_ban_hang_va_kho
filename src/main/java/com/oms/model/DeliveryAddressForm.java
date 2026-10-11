package com.oms.model;

// Dữ liệu form thêm/sửa điểm giao hàng (S3-04), giữ nguyên chữ người dùng gõ để hiện lại khi có lỗi
public class DeliveryAddressForm {

    private final Long id;
    private final String label;
    private final String address;
    private final String receiverName;
    private final String receiverPhone;
    private final String routeNote;
    private final boolean makeDefault;

    public DeliveryAddressForm(Long id, String label, String address, String receiverName, String receiverPhone,
                               String routeNote, boolean makeDefault) {
        this.id = id;
        this.label = label;
        this.address = address;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.routeNote = routeNote;
        this.makeDefault = makeDefault;
    }

    public static DeliveryAddressForm empty() {
        return new DeliveryAddressForm(null, null, null, null, null, null, false);
    }

    public static DeliveryAddressForm of(DeliveryAddressEntry entry) {
        return new DeliveryAddressForm(entry.getId(), entry.getLabel(), entry.getAddress(), entry.getReceiverName(),
                entry.getReceiverPhone(), entry.getRouteNote(), entry.isDefaultAddress());
    }

    // id = null: thêm điểm giao mới
    public Long getId() {
        return id;
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

    public boolean isMakeDefault() {
        return makeDefault;
    }
}
