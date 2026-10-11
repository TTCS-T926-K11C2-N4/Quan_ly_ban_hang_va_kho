package com.oms.service;

import com.oms.dao.PriceHistoryDao;
import com.oms.model.PageResult;
import com.oms.model.PriceHistoryFilter;
import com.oms.model.PriceHistoryRow;
import com.oms.util.DateTimeUtil;

import java.sql.SQLException;
import java.time.LocalDate;

// Lịch sử thay đổi giá (S3-02): chỉ đọc. Giá cũ, giá mới, người sửa, ngày áp dụng lấy từ price_history;
// bảng này không sửa / xoá được (trigger trong CSDL) nên lịch sử luôn đúng như lúc ghi.
public class PriceHistoryService {

    public static final int PAGE_SIZE = 20;

    private final PriceHistoryDao priceHistoryDao = new PriceHistoryDao();

    public PageResult<PriceHistoryRow> search(PriceHistoryFilter filter, int requestedPage) throws SQLException {
        LocalDate today = DateTimeUtil.today();
        long total = priceHistoryDao.count(filter, today);
        int totalPages = (int) Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(Math.max(requestedPage, 1), totalPages);
        return new PageResult<>(priceHistoryDao.findPage(filter, today, (page - 1) * PAGE_SIZE, PAGE_SIZE), page,
                PAGE_SIZE, total);
    }
}
