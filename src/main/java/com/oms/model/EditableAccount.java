package com.oms.model;

import java.util.List;

// Tài khoản đang sửa: dữ liệu hiện tại trong DB (điền sẵn vào form) + trạng thái và vai trò gốc
public class EditableAccount {

    private final long id;
    private final String status;
    private final List<String> allRoleCodes;
    private final AccountForm form;

    public EditableAccount(long id, String status, List<String> allRoleCodes, AccountForm form) {
        this.id = id;
        this.status = status;
        this.allRoleCodes = allRoleCodes;
        this.form = form;
    }

    public long getId() {
        return id;
    }

    public String getUsername() {
        return form.getUsername();
    }

    public AccountForm getForm() {
        return form;
    }

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    // Vai trò Đại lý không có trong form nên cần biết để giữ lại khi lưu
    public boolean hasCustomerRole() {
        return allRoleCodes.contains("CUSTOMER");
    }
}
