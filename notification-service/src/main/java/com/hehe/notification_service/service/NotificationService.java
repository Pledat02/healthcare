package com.hehe.notification_service.service;

import com.hehe.notification_service.dto.event.AppointmentNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final EmailService emailService;
    private final EmailTemplateBuilder templateBuilder;

    public void handle(AppointmentNotificationEvent e) {
        String subject; String html;
        switch (e.getType()) {
            case APPOINTMENT_CREATED   -> { subject = "Đặt lịch thành công";      html = templateBuilder.build(e); }
            case APPOINTMENT_CONFIRMED -> { subject = "Lịch khám đã được xác nhận"; html = templateBuilder.build(e); }
            case APPOINTMENT_CANCELLED -> { subject = "Lịch khám đã bị hủy";        html = templateBuilder.build(e); }
            case APPOINTMENT_COMPLETED -> { subject = "Cảm ơn bạn đã đến khám";     html = templateBuilder.build(e); }
            default -> { return; }
        }
        emailService.sendHtml(e.getPatientEmail(), subject, html);
    }
}
