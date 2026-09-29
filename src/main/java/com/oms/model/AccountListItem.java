package com.oms.model;

import java.util.ArrayList;
import java.util.List;

public class AccountListItem {

    private final long id;
    private final String fullName;
    private final String username;
    private final String phone;
    private final AccountStatus status;
    private final List<Role> roles = new ArrayList<>();

    public AccountListItem(long id, String fullName, String username, String phone, AccountStatus status) {
        this.id = id;
        this.fullName = fullName;
        this.username = username;
        this.phone = phone;
        this.status = status;
    }

    public long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getPhone() {
        return phone;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public List<Role> getRoles() {
        return roles;
    }
}
