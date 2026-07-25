package com.hehe.appointment_service.dto.event;

public enum NotificationType {
    APPOINTMENT_CREATED,     // vừa đặt (chờ duyệt)
    APPOINTMENT_CONFIRMED,   // bác sĩ/admin xác nhận
    APPOINTMENT_CANCELLED,   // bị hủy
    APPOINTMENT_COMPLETED    // đã khám xong
}
