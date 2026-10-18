package com.oms.service;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// S7-04: mỗi ngày một lần (00:10) đánh giá lại cờ công nợ của mọi đại lý; chạy thêm một lần sau khi ứng dụng khởi động
// để bù lần chạy bị lỡ khi máy chủ tắt. Lần chạy sau mỗi lần thu tiền do PaymentService gọi.
@WebListener
public class CreditFlagJob implements ServletContextListener {

    private static final LocalTime DAILY_AT = LocalTime.of(0, 10);
    private static final long STARTUP_DELAY_MINUTES = 2;

    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        CreditFlagService flagService = new CreditFlagService();
        Runnable task = () -> {
            try {
                int changed = flagService.evaluateAll();
                context.log("Đã đánh giá lại cờ công nợ, " + changed + " đại lý đổi cờ");
            } catch (Exception e) {
                // Lỗi một lần (vd mất kết nối CSDL) không được làm dừng lịch chạy các lần sau
                context.log("Không đánh giá lại được cờ công nợ", e);
            }
        };
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "credit-flag");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.schedule(task, STARTUP_DELAY_MINUTES, TimeUnit.MINUTES);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.toLocalDate().atTime(DAILY_AT);
        if (!next.isAfter(now)) {
            next = next.plusDays(1);
        }
        scheduler.scheduleAtFixedRate(task, Duration.between(now, next).toMinutes(), TimeUnit.DAYS.toMinutes(1),
                TimeUnit.MINUTES);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }
}
