package com.hehe.appointment_service.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    // --- Chung ---
    APPOINTMENT_NOT_FOUND(404, "Không tìm thấy lịch hẹn"),
    INVALID_INPUT(400, "Dữ liệu không hợp lệ"),
    INVALID_DATE_RANGE(400, "Ngày bắt đầu phải nhỏ hơn hoặc bằng ngày kết thúc"),
    INTERNAL_ERROR(500, "Lỗi hệ thống"),

    // --- US-05: đặt lịch ---
    PATIENT_NOT_FOUND(404, "Không tìm thấy bệnh nhân"),
    DOCTOR_NOT_FOUND(404, "Không tìm thấy bác sĩ"),

    // --- BR-02: không đặt lịch trong quá khứ ---
    APPOINTMENT_TIME_IN_PAST(400, "Không thể đặt lịch vào thời điểm trong quá khứ"),

    // --- BR-03: phải trong giờ làm việc của bác sĩ ---
    OUTSIDE_WORKING_HOURS(400, "Giờ hẹn nằm ngoài giờ làm việc của bác sĩ"),

    // --- BR-01: chống trùng lịch ---
    APPOINTMENT_CONFLICT(409, "Bác sĩ đã có lịch hẹn khác trong khung giờ này"),

    // --- BR-04: lịch đã khám xong thì khóa ---
    CANNOT_MODIFY_COMPLETED(400, "Lịch hẹn đã hoàn thành, không thể hủy hoặc sửa"),

    // --- BR-06: chỉ truy cập dữ liệu của mình ---
    FORBIDDEN(403, "Bạn không có quyền truy cập lịch hẹn này"),

    // --- Trạng thái (mục 14 - vòng đời lịch hẹn) ---
    INVALID_STATUS_TRANSITION(400, "Chuyển trạng thái không hợp lệ"),
    APPOINTMENT_ALREADY_CANCELLED(400, "Lịch hẹn đã bị hủy trước đó"),
    APPOINTMENT_NOT_CONFIRMED(400, "Chỉ có thể hoàn thành lịch hẹn đã được xác nhận"),
    APPOINTMENT_ALREADY_COMPLETED(400, "Lịch hẹn đã hoàn thành, không thể xác nhận lại"),

    // --- Gọi service khác ---
    DOCTOR_SERVICE_UNAVAILABLE(503, "Không kết nối được doctor-service");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
