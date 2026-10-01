package com.oms.util;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

// Gửi email qua SMTP (Gmail: dùng App Password). Cấu hình lấy từ biến môi trường trong setenv.bat của Tomcat:
//   SMTP_USER, SMTP_PASSWORD (bắt buộc); SMTP_HOST (mặc định smtp.gmail.com), SMTP_PORT (mặc định 587),
//   SMTP_FROM (mặc định bằng SMTP_USER).
// Gửi trên luồng riêng để thời gian phản hồi không để lộ email có tồn tại trong hệ thống hay không (S1-03).
@WebListener
public class MailSender implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(MailSender.class.getName());
    private static final String SENDER_NAME = "Hệ thống quản lý bán hàng & kho";
    private static final int TIMEOUT_MILLIS = 15_000;

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "mail-sender");
        thread.setDaemon(true);
        return thread;
    });

    public static void sendAsync(String to, String subject, String body) {
        EXECUTOR.execute(() -> {
            try {
                send(to, subject, body);
            } catch (MessagingException e) {
                LOGGER.log(Level.SEVERE, "Không gửi được email tới " + to, e);
            }
        });
    }

    // Gửi ngay trên luồng hiện tại; dùng khi phải biết chắc thư đã gửi được (vd gửi mật khẩu tạm của tài khoản mới)
    public static void send(String to, String subject, String body) throws MessagingException {
        String user = System.getenv("SMTP_USER");
        String password = System.getenv("SMTP_PASSWORD");
        if (user == null || password == null) {
            throw new MessagingException("Chưa cấu hình biến môi trường SMTP_USER / SMTP_PASSWORD");
        }
        String from = envOrDefault("SMTP_FROM", user);

        Properties properties = new Properties();
        properties.put("mail.smtp.host", envOrDefault("SMTP_HOST", "smtp.gmail.com"));
        properties.put("mail.smtp.port", envOrDefault("SMTP_PORT", "587"));
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.starttls.required", "true");
        properties.put("mail.smtp.connectiontimeout", String.valueOf(TIMEOUT_MILLIS));
        properties.put("mail.smtp.timeout", String.valueOf(TIMEOUT_MILLIS));
        properties.put("mail.smtp.writetimeout", String.valueOf(TIMEOUT_MILLIS));

        Session session = Session.getInstance(properties, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, password);
            }
        });

        MimeMessage message = new MimeMessage(session);
        try {
            message.setFrom(new InternetAddress(from, SENDER_NAME, StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException e) {
            throw new MessagingException("Không mã hoá được tên người gửi", e);
        }
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        message.setSubject(subject, StandardCharsets.UTF_8.name());
        message.setText(body, StandardCharsets.UTF_8.name());
        Transport.send(message);
    }

    private static String envOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    // Dừng luồng gửi khi gỡ ứng dụng để Tomcat không báo rò rỉ luồng
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        EXECUTOR.shutdown();
    }
}
