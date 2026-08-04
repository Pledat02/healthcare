package com.hehe.doctor_service.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {
    AVATAR_EMPTY(400, "Vui long chon anh dai dien"),
    AVATAR_TOO_LARGE(413, "Anh dai dien vuot qua 5 MB"),
    AVATAR_INVALID(415, "Anh khong hop le; chi chap nhan JPEG hoac PNG"),
    AVATAR_NOT_PENDING(409, "Bac si khong co anh dang cho duyet"),
    AVATAR_STORAGE_UNAVAILABLE(503, "Kho anh tam thoi khong kha dung"),
    FORBIDDEN(403, "Bạn không có quyền truy cập dữ liệu này"),
    DOCTOR_NOT_FOUND(404, "Không tìm thấy bác sĩ"),
    TOO_MANY_IDS(400, "Số lượng ID vượt giới hạn cho phép (tối đa 100)"),

    // US-03: tao tai khoan dang nhap cho bac si tren Keycloak
    USERNAME_ALREADY_EXISTS(409, "Tên đăng nhập đã tồn tại"),
    KEYCLOAK_UNAVAILABLE(503, "Không kết nối được hệ thống tài khoản (Keycloak)"),

    // Nghi phep bac si
    LEAVE_DATE_IN_PAST(400, "Không thể đăng ký nghỉ cho ngày trong quá khứ"),
    LEAVE_ALREADY_EXISTS(409, "Bạn đã đăng ký nghỉ cho ngày này"),
    LEAVE_NOT_FOUND(404, "Không tìm thấy ngày nghỉ");


    private final int code;
    private final String message;
    ErrorCode(int code, String message){
        this.code = code;
        this.message=message;
    }

}
