package com.oms.service;

import com.oms.dao.StockCardDao;
import com.oms.model.PageResult;
import com.oms.model.SelectOption;
import com.oms.model.StockCardEntry;
import com.oms.model.StockCardFilter;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

// Thẻ kho của một sản phẩm (S6-02): mọi phát sinh nhập, xuất, chuyển, điều chỉnh theo thứ tự thời gian, mỗi dòng có
// tồn sau phát sinh và liên kết tới chứng từ gốc; lọc theo kho và khoảng thời gian.
public class StockCardService {

    public static final int PAGE_SIZE = 50;

    private final StockCardDao cardDao = new StockCardDao();

    public List<SelectOption> getProducts() throws SQLException {
        return cardDao.findProducts();
    }

    public PageResult<StockCardEntry> search(StockCardFilter filter, int requestedPage) throws SQLException {
        long total = cardDao.count(filter);
        int totalPages = (int) Math.max(1, (total + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(Math.max(1, requestedPage), totalPages);
        return new PageResult<>(cardDao.findPage(filter, (page - 1) * PAGE_SIZE, PAGE_SIZE), page, PAGE_SIZE, total);
    }

    public BigDecimal currentOnHand(StockCardFilter filter) throws SQLException {
        return cardDao.findCurrentOnHand(filter.getProductId(), filter.getWarehouseId());
    }

    public String baseUnitName(long productId) throws SQLException {
        return cardDao.findBaseUnitName(productId);
    }
}
