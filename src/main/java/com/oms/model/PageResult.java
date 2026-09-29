package com.oms.model;

import java.util.List;

public class PageResult<T> {

    private static final int PAGE_LINK_COUNT = 5;

    private final List<T> items;
    private final int page;
    private final int pageSize;
    private final long totalItems;

    public PageResult(List<T> items, int page, int pageSize, long totalItems) {
        this.items = items;
        this.page = page;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
    }

    public List<T> getItems() {
        return items;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public int getTotalPages() {
        return Math.max(1, (int) ((totalItems + pageSize - 1) / pageSize));
    }

    // STT của dòng đầu tiên trên trang hiện tại
    public int getFirstRowNumber() {
        return (page - 1) * pageSize + 1;
    }

    // Cửa sổ tối đa 5 số trang quanh trang hiện tại, vd trang 7/20 -> 5..9
    public int getStartPage() {
        int start = Math.max(1, page - PAGE_LINK_COUNT / 2);
        return Math.max(1, Math.min(start, getTotalPages() - PAGE_LINK_COUNT + 1));
    }

    public int getEndPage() {
        return Math.min(getTotalPages(), getStartPage() + PAGE_LINK_COUNT - 1);
    }
}
