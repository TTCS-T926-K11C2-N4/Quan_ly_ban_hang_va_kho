package com.oms.model;

// Điểm giao hàng của đại lý (customer_delivery_addresses). Màn khai báo điểm giao thuộc S3-04;
// màn tạo đơn (S3-09) chỉ chọn trong danh sách điểm giao đang hoạt động của đúng đại lý đó.
public class DeliveryAddress {

    private final long id;
    private final String label;
    private final String address;
    private final String receiverName;
    private final boolean defaultAddress;

    public DeliveryAddress(long id, String label, String address, String receiverName, boolean defaultAddress) {
        this.id = id;
        this.label = label;
        this.address = address;
        this.receiverName = receiverName;
        this.defaultAddress = defaultAddress;
    }

    public long getId() {
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

    public boolean isDefaultAddress() {
        return defaultAddress;
    }

    // Chữ trong ô chọn điểm giao, vd "Cửa hàng chính - 25 Nguyễn Trãi, Hà Nội"
    public String getText() {
        return label == null || label.isBlank() ? address : label + " - " + address;
    }
}
