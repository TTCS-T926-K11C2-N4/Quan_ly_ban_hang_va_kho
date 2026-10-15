package com.oms.model;

import com.oms.util.DateTimeUtil;

import java.time.LocalDate;
import java.util.List;

// Danh sách giao hàng của một ngày (S6-08): các điểm giao và tiến độ giao của mọi phiếu xuất trong ngày
public class DeliveryDay {

    private final LocalDate date;
    private final List<DeliveryPoint> points;
    private final int totalNotes;
    private final int deliveredNotes;
    private final int failedNotes;

    // points đã lọc theo từ khoá / trạng thái; các con số tổng tính trên mọi phiếu của ngày
    public DeliveryDay(LocalDate date, List<DeliveryPoint> points, int totalNotes, int deliveredNotes,
                       int failedNotes) {
        this.date = date;
        this.points = points;
        this.totalNotes = totalNotes;
        this.deliveredNotes = deliveredNotes;
        this.failedNotes = failedNotes;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getDateText() {
        return DateTimeUtil.formatDay(date);
    }

    public boolean isToday() {
        return date.equals(DateTimeUtil.today());
    }

    public List<DeliveryPoint> getPoints() {
        return points;
    }

    public int getTotalNotes() {
        return totalNotes;
    }

    public int getDeliveredNotes() {
        return deliveredNotes;
    }

    public int getFailedNotes() {
        return failedNotes;
    }

    public int getHandledNotes() {
        return deliveredNotes + failedNotes;
    }

    public int getPendingNotes() {
        return totalNotes - getHandledNotes();
    }

    public int getProgressPercent() {
        return totalNotes == 0 ? 0 : getHandledNotes() * 100 / totalNotes;
    }
}
