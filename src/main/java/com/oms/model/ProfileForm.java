package com.oms.model;

// Hai trường người dùng tự sửa được trên trang Hồ sơ cá nhân (S2-02); các trường khác chỉ hiển thị
public class ProfileForm {

    private final String fullName;
    private final String phone;

    public ProfileForm(String fullName, String phone) {
        this.fullName = fullName;
        this.phone = phone;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }
}
