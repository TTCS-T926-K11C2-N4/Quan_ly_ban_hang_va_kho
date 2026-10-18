package com.oms.service;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// SCRUM-65, SCRUM-81: mỗi giờ báo nhân viên kinh doanh về các đơn quá ngày giao mong muốn mà chưa giao xong.
// Chạy trong Tomcat cùng ứng dụng; dừng khi ứng dụng dừng.
@WebListener
public class OverdueDeliveryJob implements ServletContextListener {

    private static final long INITIAL_DELAY_MINUTES = 2;
    private static final long PERIOD_MINUTES = 60;

    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        OverdueDeliveryService service = new OverdueDeliveryService();
        scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "overdue-delivery");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                int orders = service.notifyOverdue();
                if (orders > 0) {
                    context.log("Đã báo quá ngày giao cho " + orders + " đơn");
                }
            } catch (Exception e) {
                // Lỗi một lần (vd mất kết nối CSDL) không được làm dừng lịch chạy các lần sau
                context.log("Không báo được đơn quá ngày giao", e);
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
