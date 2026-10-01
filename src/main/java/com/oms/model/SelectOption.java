package com.oms.model;

// Một lựa chọn trong ô select (kho, địa bàn...)
public class SelectOption {

    private final long id;
    private final String code;
    private final String name;

    public SelectOption(long id, String name) {
        this(id, null, name);
    }

    // code: mã ngắn (vd KHO-TT) để người dùng ghi trong file Excel thay cho id
    public SelectOption(long id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
