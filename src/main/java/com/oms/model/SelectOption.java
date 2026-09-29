package com.oms.model;

// Một lựa chọn trong ô select (kho, địa bàn...)
public class SelectOption {

    private final long id;
    private final String name;

    public SelectOption(long id, String name) {
        this.id = id;
        this.name = name;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
