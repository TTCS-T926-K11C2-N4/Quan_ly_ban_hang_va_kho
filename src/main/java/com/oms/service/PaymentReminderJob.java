package com.oms.service;

import com.oms.util.DateTimeUtil;
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

// S7-08: mỗi ngày một lần (08:00) gửi nhắc hạn thanh toán; chạy thêm một lần sau khi ứng dụng khởi động để bù lần lỡ.
// Chạy lại không gửi trùng nhờ payment_reminder_logs.
@WebListener
public class PaymentReminderJob implements ServletContextListener {

    private static final LocalTime DAILY_AT = LocalTime.of(8, 0);
    private static final long STARTUP_DELAY_MINUTES = 3;

    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        PaymentReminderService reminderService = new PaymentReminderService();
        Runnable task = () -> {
            try {
                int sent = reminderService.run(DateTimeUtil.today());
                if (sent > 0) {
                    context.log("Đã gửi " + sent + " nhắc hạn thanh toán");
                }
            } catch (Exception e) {
                // Lỗi một lần (vd mất kết nối CSDL) không được làm dừng lịch chạy các lần sau
                context.log("Không gửi được nhắc hạn thanh toán", e);
            }
        };
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "payment-reminder");
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
