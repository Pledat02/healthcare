package com.hehe.doctor_service.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    FORBIDDEN(403, "Bạn không có quyền truy cập dữ liệu này"),
     DOCTOR_NOT_FOUND(404,"Không tìm thấy bác sĩ");


    private final int code;
    private final String message;
    ErrorCode(int code, String message){
        this.code = code;
        this.message=message;
    }

}
