package com.oms.util;

import org.mindrot.jbcrypt.BCrypt;

import java.security.SecureRandom;

public final class PasswordUtil {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 64;

    private static final int BCRYPT_COST = 10;
    private static final int TEMPORARY_LENGTH = 10;
    // Bỏ các ký tự dễ nhầm khi đọc cho người dùng (0/O, 1/l/I)
    private static final String TEMPORARY_ALPHABET =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    // Quy tắc chung (khớp change-password.js): 8–64 ký tự, có chữ (kể cả chữ có dấu) và số.
    // Giới hạn 64 vì bcrypt chỉ dùng 72 byte đầu của mật khẩu.
    public static boolean meetsPolicy(String password) {
        return password != null
                && password.length() >= MIN_LENGTH && password.length() <= MAX_LENGTH
                && password.matches(".*\\p{L}.*") && password.matches(".*\\d.*");
    }

    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_COST));
    }

    // Hash hỏng trong DB (không phải bcrypt) thì coi như sai mật khẩu thay vì văng lỗi
    public static boolean matches(String password, String passwordHash) {
        if (password == null || passwordHash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(password, passwordHash);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    // Luôn có cả chữ và số để khớp quy tắc mật khẩu ở trang Đổi mật khẩu
    public static String generateTemporary() {
        String password;
        do {
            StringBuilder builder = new StringBuilder(TEMPORARY_LENGTH);
            for (int i = 0; i < TEMPORARY_LENGTH; i++) {
                builder.append(TEMPORARY_ALPHABET.charAt(RANDOM.nextInt(TEMPORARY_ALPHABET.length())));
            }
            password = builder.toString();
        } while (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*"));
        return password;
    }
}
