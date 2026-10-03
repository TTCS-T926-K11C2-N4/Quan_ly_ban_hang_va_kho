package com.oms.model;

import java.util.List;

// Dữ liệu form Thêm/Sửa/Tạo phiên bản bảng giá, giữ nguyên chuỗi người dùng nhập để hiện lại khi có lỗi
public class PriceListForm {

    // Một dòng giá trong form; productId = null nếu chưa chọn sản phẩm
    public static class Line {
        private final Long productId;
        private final String price;
        private final String floorPrice;

        public Line(Long productId, String price, String floorPrice) {
            this.productId = productId;
            this.price = price;
            this.floorPrice = floorPrice;
        }

        public Long getProductId() {
            return productId;
        }

        public String getPrice() {
            return price;
        }

        public String getFloorPrice() {
            return floorPrice;
        }
    }

    private final Long customerGroupId;
    private final String name;
    private final String validFrom;
    private final String validTo;
    private final List<Line> lines;

    // validFrom/validTo dạng yyyy-MM-dd (ô input type="date")
    public PriceListForm(Long customerGroupId, String name, String validFrom, String validTo, List<Line> lines) {
        this.customerGroupId = customerGroupId;
        this.name = name;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.lines = lines;
    }

    public Long getCustomerGroupId() {
        return customerGroupId;
    }

    public String getName() {
        return name;
    }

    public String getValidFrom() {
        return validFrom;
    }

    public String getValidTo() {
        return validTo;
    }

    public List<Line> getLines() {
        return lines;
    }
}
