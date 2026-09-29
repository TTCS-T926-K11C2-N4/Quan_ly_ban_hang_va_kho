package com.oms.model;

public class Role {

    private final String code;
    private final String name;

    public Role(String code, String name) {
        this.code = code;
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }
}
