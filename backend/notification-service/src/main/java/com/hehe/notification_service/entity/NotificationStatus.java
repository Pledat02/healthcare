package com.hehe.notification_service.entity;

public enum NotificationStatus {
    PENDING,    // dang cho toi gio gui (nhac lich)
    PROCESSING, // da duoc mot worker claim, dang gui
    SENT,       // da gui thanh cong
    FAILED,     // gui that bai
    CANCELLED   // khong gui nua (vd lich hen bi huy truoc khi toi gio nhac)
}
