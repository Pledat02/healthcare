package com.hehe.notification_service.service;

import com.hehe.notification_service.dto.event.AppointmentNotificationEvent;
import com.hehe.notification_service.dto.event.NotificationType;
import com.hehe.notification_service.entity.Notification;
import com.hehe.notification_service.entity.NotificationStatus;
import com.hehe.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final EmailService emailService;
    private final EmailTemplateBuilder templateBuilder;
    private final NotificationRepository notificationRepository;

    @Value("${notification.reminder-hours-before}")
    private int reminderHoursBefore;

    public void handle(AppointmentNotificationEvent e) {
        String subject = switch (e.getType()) {
            case APPOINTMENT_CREATED   -> "Đặt lịch khám thành công";
            case APPOINTMENT_CONFIRMED -> "Lịch khám đã được xác nhận";
            case APPOINTMENT_CANCELLED -> "Lịch khám đã bị hủy";
            case APPOINTMENT_COMPLETED -> "Cảm ơn bạn đã đến khám";
            default -> null;
        };
        if (subject == null) return;

        // Gui mail ngay + ghi nhat ky
        save(e, e.getType(), Instant.now(), NotificationStatus.SENT);
        emailService.sendHtml(e.getPatientEmail(), subject, templateBuilder.build(e));

        // US-09: dat lich thanh cong -> len lich nhac truoc 24h
        if (e.getType() == NotificationType.APPOINTMENT_CREATED) {
            scheduleReminder(e);
        }
        // Lich bi huy -> khong nhac nua
        if (e.getType() == NotificationType.APPOINTMENT_CANCELLED) {
            cancelReminders(e.getAppointmentId());
        }
    }

    /** Tao ban ghi REMINDER trang thai PENDING; den gio job se gui */
    private void scheduleReminder(AppointmentNotificationEvent e) {
        if (e.getAppointmentTime() == null || e.getAppointmentId() == null) return;

        Instant remindAt = e.getAppointmentTime().minus(reminderHoursBefore, ChronoUnit.HOURS);
        if (remindAt.isBefore(Instant.now())) {
            // Dat lich sat gio (duoi 24h) -> khong con kip nhac truoc
            log.info("Lich {} dien ra trong vong {}h, bo qua nhac lich",
                    e.getAppointmentId(), reminderHoursBefore);
            return;
        }
        if (notificationRepository.existsByAppointmentIdAndType(
                e.getAppointmentId(), NotificationType.REMINDER)) {
            return;
        }
        save(e, NotificationType.REMINDER, remindAt, NotificationStatus.PENDING);
        log.info("Da len lich nhac cho {} luc {}", e.getAppointmentId(), remindAt);
    }

    private void cancelReminders(String appointmentId) {
        if (appointmentId == null) return;
        List<Notification> pending = notificationRepository.findByAppointmentIdAndTypeAndStatus(
                appointmentId, NotificationType.REMINDER, NotificationStatus.PENDING);
        pending.forEach(n -> n.setStatus(NotificationStatus.CANCELLED));
        notificationRepository.saveAll(pending);
    }

    private void save(AppointmentNotificationEvent e, NotificationType type,
                      Instant scheduledAt, NotificationStatus status) {
        notificationRepository.save(Notification.builder()
                .appointmentId(e.getAppointmentId())
                .recipientEmail(e.getPatientEmail())
                .type(type)
                .status(status)
                .scheduledAt(scheduledAt)
                .sentAt(status == NotificationStatus.SENT ? Instant.now() : null)
                .appointmentTime(e.getAppointmentTime())
                .patientName(e.getPatientName())
                .doctorName(e.getDoctorName())
                .specialization(e.getSpecialization())
                .reason(e.getReason())
                .build());
    }

    public List<Notification> findAll(NotificationStatus status) {
        return status == null
                ? notificationRepository.findAllByOrderByScheduledAtDesc()
                : notificationRepository.findByStatusOrderByScheduledAtDesc(status);
    }
}
