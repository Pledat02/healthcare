package com.hehe.doctor_service.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    FORBIDDEN(403, "Bạn không có quyền truy cập dữ liệu này"),
    DOCTOR_NOT_FOUND(404, "Không tìm thấy bác sĩ"),
    TOO_MANY_IDS(400, "Số lượng ID vượt giới hạn cho phép (tối đa 100)"),

    // US-03: tao tai khoan dang nhap cho bac si tren Keycloak
    USERNAME_ALREADY_EXISTS(409, "Tên đăng nhập đã tồn tại"),
    KEYCLOAK_UNAVAILABLE(503, "Không kết nối được hệ thống tài khoản (Keycloak)");


    private final int code;
    private final String message;
    ErrorCode(int code, String message){
        this.code = code;
        this.message=message;
    }

}
