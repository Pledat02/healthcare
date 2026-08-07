package com.hehe.security_service.dto;

public record ApiResponse<T>(int code, String message, T data) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(200, "Thành công", data);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(201, "Đã tạo", data);
    }
}
