package com.hehe.patient_service.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    PATIENT_NOT_FOUND(404, "Không tìm thấy bệnh nhân"),
    INVALID_INPUT(400, "Dữ liệu không hợp lệ"),
    PHONE_ALREADY_EXISTS(409, "Số điện thoại đã tồn tại"),
    FORBIDDEN(403, "Bạn không có quyền truy cập dữ liệu này"),
    PATIENT_ALREADY_EXISTS(409, "Hồ sơ bệnh nhân đã tồn tại"),
    INTERNAL_ERROR(500, "Lỗi hệ thống");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}