package com.oms.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

// Thông tin kết nối lấy từ biến môi trường (đặt trong setenv.bat của Tomcat), không để trong code
public final class DbConnection {

    private static final String URL = System.getenv("DB_URL");
    private static final String USER = System.getenv("DB_USER");
    private static final String PASSWORD = System.getenv("DB_PASSWORD");

    static {
        try {
            // Tomcat không tự nạp driver JDBC nằm trong WEB-INF/lib qua ServiceLoader ở mọi trường hợp
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DbConnection() {
    }

    public static Connection getConnection() throws SQLException {
        if (URL == null || USER == null) {
            throw new SQLException("Chưa cấu hình biến môi trường DB_URL / DB_USER / DB_PASSWORD");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
