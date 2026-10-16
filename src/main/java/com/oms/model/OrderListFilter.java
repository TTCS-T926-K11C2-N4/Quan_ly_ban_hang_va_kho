package com.oms.model;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// Điều kiện tìm kiếm, lọc danh sách đơn hàng (S4-07); null nghĩa là không lọc theo tiêu chí đó.
// Không chọn trạng thái thì danh sách chỉ gồm đơn đã gửi (không có Nháp); muốn xem Nháp thì lọc đúng trạng thái Nháp.
public class OrderListFilter {

    public static final List<Integer> PAGE_SIZES = List.of(10, 20, 50);
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final String SORT_NEWEST = "newest";
    // Khoá sắp xếp -> nhãn hiện trên giao diện (LinkedHashMap giữ thứ tự)
    private static final Map<String, String> SORTS = new java.util.LinkedHashMap<>();

    static {
        SORTS.put(SORT_NEWEST, "Mới nhất");
        SORTS.put("oldest", "Cũ nhất");
        SORTS.put("total_desc", "Tổng tiền giảm dần");
        SORTS.put("total_asc", "Tổng tiền tăng dần");
    }

    private final String keyword;
    private final String status;
    private final Long customerId;
    private final Long salesRepId;
    private final Long regionId;
    private final LocalDate dateFrom;
    private final LocalDate dateTo;
    private final String sort;
    private final int pageSize;

    public OrderListFilter(String keyword, String status, Long customerId, Long salesRepId, Long regionId,
                           LocalDate dateFrom, LocalDate dateTo, String sort, int pageSize) {
        this.keyword = keyword;
        this.status = status != null && OrderStatus.isKnown(status) ? status : null;
        this.customerId = customerId;
        this.salesRepId = salesRepId;
        this.regionId = regionId;
        // Khoảng ngày đảo ngược thì đổi chỗ để vẫn ra kết quả thay vì rỗng khó hiểu
        boolean swapped = dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo);
        this.dateFrom = swapped ? dateTo : dateFrom;
        this.dateTo = swapped ? dateFrom : dateTo;
        this.sort = SORTS.containsKey(sort) ? sort : SORT_NEWEST;
        this.pageSize = PAGE_SIZES.contains(pageSize) ? pageSize : DEFAULT_PAGE_SIZE;
    }

    public static Map<String, String> getSorts() {
        return SORTS;
    }

    // Đang có điều kiện lọc nào (không tính cách sắp xếp, số dòng mỗi trang)
    public boolean isFiltered() {
        return getActiveFilterCount() > 0 || keyword != null;
    }

    // Số ô lọc đang chọn (không tính ô tìm kiếm), hiện cạnh nút Bộ lọc trên điện thoại
    public int getActiveFilterCount() {
        return (status == null ? 0 : 1) + (customerId == null ? 0 : 1) + (salesRepId == null ? 0 : 1)
                + (regionId == null ? 0 : 1) + (dateFrom == null ? 0 : 1) + (dateTo == null ? 0 : 1);
    }

    public String getKeyword() {
        return keyword;
    }

    public String getStatus() {
        return status;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public Long getSalesRepId() {
        return salesRepId;
    }

    public Long getRegionId() {
        return regionId;
    }

    public LocalDate getDateFrom() {
        return dateFrom;
    }

    public LocalDate getDateTo() {
        return dateTo;
    }

    public String getSort() {
        return sort;
    }

    public int getPageSize() {
        return pageSize;
    }
}
