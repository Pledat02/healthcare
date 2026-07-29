package com.hehe.notification_service.dto.event;

public enum NotificationType {
    APPOINTMENT_CREATED,     // vừa đặt (chờ duyệt)
    APPOINTMENT_RESCHEDULED, // bệnh nhân đổi ngày/giờ khám
    APPOINTMENT_CONFIRMED,   // bác sĩ/admin xác nhận
    APPOINTMENT_CANCELLED,   // bị hủy
    APPOINTMENT_COMPLETED,   // đã khám xong
    REMINDER                 // US-09: nhắc trước 24h (job tu tao, khong den tu event)
}
