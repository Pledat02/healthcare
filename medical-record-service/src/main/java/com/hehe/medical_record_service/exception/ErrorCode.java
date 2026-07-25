package com.hehe.medical_record_service.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // --- Chung ---
    MEDICAL_RECORD_NOT_FOUND(404, "Không tìm thấy hồ sơ khám"),
    INVALID_INPUT(400, "Dữ liệu không hợp lệ"),
    INTERNAL_ERROR(500, "Lỗi hệ thống"),

    // --- Tham chiếu tới service khác (gọi qua API) ---
    APPOINTMENT_NOT_FOUND(404, "Không tìm thấy lịch hẹn"),
    PATIENT_NOT_FOUND(404, "Không tìm thấy bệnh nhân"),
    DOCTOR_NOT_FOUND(404, "Không tìm thấy bác sĩ"),

    // --- BR-05: chỉ tạo hồ sơ cho lịch hẹn đã COMPLETED ---
    APPOINTMENT_NOT_COMPLETED(400, "Chỉ tạo được hồ sơ khám cho lịch hẹn đã hoàn thành"),

    // --- US-10: chỉ bác sĩ khám buổi đó mới được ghi ---
    NOT_THE_TREATING_DOCTOR(403, "Chỉ bác sĩ khám buổi đó mới được ghi hồ sơ"),

    // --- 1 lịch hẹn chỉ có 1 hồ sơ khám ---
    MEDICAL_RECORD_ALREADY_EXISTS(409, "Lịch hẹn này đã có hồ sơ khám"),

    // --- BR-06: chỉ truy cập dữ liệu của mình ---
    FORBIDDEN(403, "Bạn không có quyền truy cập hồ sơ khám này"),

    // --- Gọi service khác thất bại ---
    SERVICE_UNAVAILABLE(503, "Không kết nối được service phụ thuộc");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
