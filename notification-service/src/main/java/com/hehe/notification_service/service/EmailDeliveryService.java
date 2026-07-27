package com.hehe.notification_service.service;

import com.hehe.notification_service.entity.Notification;
import com.hehe.notification_service.entity.NotificationStatus;
import com.hehe.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

/** Gửi mail tức thì ở luồng riêng và cập nhật nhật ký theo kết quả SMTP thực tế. */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailDeliveryService {

    private final EmailService emailService;
    private final NotificationRepository notificationRepository;

    @Async
    public void deliver(String notificationId, String to, String subject, String htmlBody) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null) {
            log.warn("Không tìm thấy notification {} để gửi", notificationId);
            return;
        }

        try {
            emailService.sendHtml(to, subject, htmlBody);
            notification.setStatus(NotificationStatus.SENT);
            notification.setSentAt(Instant.now());
            notification.setErrorMessage(null);
        } catch (Exception ex) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setErrorMessage(truncate(ex.getMessage()));
            log.error("Gửi mail {} tới {} thất bại: {}", notificationId, to, ex.getMessage());
        }
        notificationRepository.save(notification);
    }

    private String truncate(String message) {
        if (message == null) return null;
        return message.length() > 500 ? message.substring(0, 500) : message;
    }
}
