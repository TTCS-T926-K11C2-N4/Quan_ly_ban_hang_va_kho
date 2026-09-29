package com.oms.model;

// Dữ liệu form Đăng ký tài khoản; mật khẩu giữ nguyên (không trim) vì khoảng trắng là một phần mật khẩu
public class RegistrationForm {

    private final String fullName;
    private final String email;
    private final String username;
    private final String password;
    private final String confirmPassword;
    private final boolean termsAccepted;

    public RegistrationForm(String fullName, String email, String username,
                            String password, String confirmPassword, boolean termsAccepted) {
        this.fullName = fullName;
        this.email = email;
        this.username = username;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.termsAccepted = termsAccepted;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public boolean isTermsAccepted() {
        return termsAccepted;
    }
}
