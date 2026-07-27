package com.hehe.notification_service.service;

import com.hehe.notification_service.dto.event.AppointmentNotificationEvent;
import com.hehe.notification_service.dto.event.NotificationType;
import com.hehe.notification_service.entity.Notification;
import com.hehe.notification_service.entity.NotificationStatus;
import com.hehe.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * US-09: he thong TU quet va gui mail nhac lich, khong can ai bam nut.
 *
 * Chi doc bang notifications cua chinh service nay (da co ban sao gio hen,
 * ten bac si...) nen job van chay dung ke ca khi appointment-service dang chet.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ReminderScheduler {

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;
    private final EmailTemplateBuilder templateBuilder;

    @Scheduled(cron = "${notification.scan-cron}")
    public void sendDueReminders() {
        List<Notification> due = notificationRepository
                .findByStatusAndScheduledAtLessThanEqual(NotificationStatus.PENDING, Instant.now());

        if (due.isEmpty()) return;
        log.info("US-09: co {} mail nhac lich den han", due.size());

        for (Notification n : due) {
            try {
                emailService.sendHtmlSync(
                        n.getRecipientEmail(),
                        "Nhắc lịch khám ngày mai",
                        templateBuilder.build(toEvent(n)));
                n.setStatus(NotificationStatus.SENT);
                n.setSentAt(Instant.now());
            } catch (Exception ex) {
                // Mot mail loi khong duoc lam dung ca lo con lai
                n.setStatus(NotificationStatus.FAILED);
                n.setErrorMessage(truncate(ex.getMessage()));
                log.error("Gui mail nhac lich {} that bai: {}", n.getId(), ex.getMessage());
            }
            notificationRepository.save(n);
        }
    }

    /** Dung lai ban sao trong DB de dung noi dung mail, khong goi service khac */
    private AppointmentNotificationEvent toEvent(Notification n) {
        return AppointmentNotificationEvent.builder()
                .type(NotificationType.REMINDER)
                .appointmentId(n.getAppointmentId())
                .patientName(n.getPatientName())
                .patientEmail(n.getRecipientEmail())
                .doctorName(n.getDoctorName())
                .specialization(n.getSpecialization())
                .appointmentTime(n.getAppointmentTime())
                .reason(n.getReason())
                .build();
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}
