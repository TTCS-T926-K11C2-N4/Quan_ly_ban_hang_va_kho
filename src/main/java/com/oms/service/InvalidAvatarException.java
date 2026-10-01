package com.oms.service;

// Ảnh tải lên không dùng được làm ảnh đại diện; message hiển thị thẳng cho người dùng
public class InvalidAvatarException extends Exception {

    public InvalidAvatarException(String message) {
        super(message);
    }
}
