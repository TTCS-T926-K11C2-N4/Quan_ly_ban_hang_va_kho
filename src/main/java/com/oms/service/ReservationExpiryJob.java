package com.oms.service;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// S5-06: cứ 5 phút nhả các giữ chỗ tồn đã quá hạn (đơn duyệt quá inventory.reservation_ttl_hours giờ chưa xuất kho)
// để tồn khả dụng trở lại cho đơn khác. Chạy trong Tomcat cùng ứng dụng; dừng khi ứng dụng dừng.
@WebListener
public class ReservationExpiryJob implements ServletContextListener {

    private static final long INITIAL_DELAY_MINUTES = 1;
    private static final long PERIOD_MINUTES = 5;

    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        StockReservationService reservationService = new StockReservationService();
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "reservation-expiry");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                int orders = reservationService.releaseExpired();
                if (orders > 0) {
                    context.log("Đã nhả giữ chỗ quá hạn của " + orders + " đơn");
                }
            } catch (Exception e) {
                // Lỗi một lần (vd mất kết nối CSDL) không được làm dừng lịch chạy các lần sau
                context.log("Không nhả được giữ chỗ quá hạn", e);
            }
        }, INITIAL_DELAY_MINUTES, PERIOD_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
