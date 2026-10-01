package com.oms.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// Một dòng người dùng đọc từ file Excel (S2-01): giá trị gốc để hiển thị/xuất báo cáo lỗi,
// form đã quy đổi vai trò/kho/địa bàn để tạo tài khoản, và danh sách lỗi của dòng
public class ImportRow implements Serializable {

    private final int index;
    private final int excelRowNumber;
    private final String fullName;
    private final String username;
    private final String email;
    private final String phone;
    private final String rolesText;
    private final String warehouseText;
    private final String regionText;
    private final String roleNames;
    private final String assignmentNames;
    private final AccountForm form;
    private final List<String> errors = new ArrayList<>();
    private boolean created;

    public ImportRow(int index, int excelRowNumber, String fullName, String username, String email, String phone,
                     String rolesText, String warehouseText, String regionText, String roleNames,
                     String assignmentNames, AccountForm form) {
        this.index = index;
        this.excelRowNumber = excelRowNumber;
        this.fullName = fullName;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.rolesText = rolesText;
        this.warehouseText = warehouseText;
        this.regionText = regionText;
        this.roleNames = roleNames;
        this.assignmentNames = assignmentNames;
        this.form = form;
    }

    // STT trong danh sách xem trước (1, 2, 3...)
    public int getIndex() {
        return index;
    }

    // Số dòng thật trong Excel (dòng 1 là tiêu đề) để người dùng tìm lại dòng lỗi
    public int getExcelRowNumber() {
        return excelRowNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    // 0987123456 -> 0987 123 456 như thiết kế; số không đủ 10 chữ số giữ nguyên
    public String getPhoneDisplay() {
        if (phone != null && phone.matches("\\d{10}")) {
            return phone.substring(0, 4) + " " + phone.substring(4, 7) + " " + phone.substring(7);
        }
        return phone;
    }

    public String getRolesText() {
        return rolesText;
    }

    public String getWarehouseText() {
        return warehouseText;
    }

    public String getRegionText() {
        return regionText;
    }

    // Tên vai trò đã nhận ra, vd "Nhân viên kho, Kế toán"; rỗng thì JSP hiện giá trị gốc
    public String getRoleNames() {
        return roleNames;
    }

    // Tên kho và/hoặc địa bàn đã nhận ra, vd "Kho Hà Nội • Hà Nội"
    public String getAssignmentNames() {
        return assignmentNames;
    }

    public AccountForm getForm() {
        return form;
    }

    public List<String> getErrors() {
        return errors;
    }

    // Mỗi lỗi đã kết thúc bằng dấu chấm nên chỉ cần nối bằng khoảng trắng
    public String getErrorText() {
        return String.join(" ", errors);
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public void addError(String error) {
        errors.add(error);
    }

    public boolean isCreated() {
        return created;
    }

    public void markCreated() {
        created = true;
    }
}
