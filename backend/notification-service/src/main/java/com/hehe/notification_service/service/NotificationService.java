package com.hehe.notification_service.service;

import com.hehe.notification_service.dto.event.AppointmentNotificationEvent;
import com.hehe.notification_service.dto.event.NotificationType;
import com.hehe.notification_service.entity.Notification;
import com.hehe.notification_service.entity.NotificationStatus;
import com.hehe.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final EmailDeliveryService emailDeliveryService;
    private final EmailTemplateBuilder templateBuilder;
    private final NotificationRepository notificationRepository;

    @Value("${notification.reminder-hours-before}")
    private int reminderHoursBefore;

    public void handle(AppointmentNotificationEvent e) {
        if (e == null || e.getType() == null) {
            log.warn("Bỏ qua sự kiện notification không hợp lệ");
            return;
        }

        String subject = switch (e.getType()) {
            case APPOINTMENT_CREATED   -> "Đặt lịch khám thành công";
            case APPOINTMENT_RESCHEDULED -> "Lịch khám đã được thay đổi";
            case APPOINTMENT_CONFIRMED -> "Lịch khám đã được xác nhận";
            case APPOINTMENT_CANCELLED -> "Lịch khám đã bị hủy";
            case APPOINTMENT_COMPLETED -> "Cảm ơn bạn đã đến khám";
            default -> null;
        };
        if (subject == null) return;

        // Không để hồ sơ thiếu email làm hỏng luồng đặt lịch.
        if (e.getPatientEmail() == null || e.getPatientEmail().isBlank()) {
            log.warn("Lịch {} không có email bệnh nhân, bỏ qua gửi mail", e.getAppointmentId());
            if (e.getType() == NotificationType.APPOINTMENT_RESCHEDULED
                    || e.getType() == NotificationType.APPOINTMENT_CANCELLED
                    || e.getType() == NotificationType.APPOINTMENT_COMPLETED) {
                cancelReminders(e.getAppointmentId());
            }
            return;
        }

        // Lưu PENDING trước; lớp gửi sẽ đổi sang SENT/FAILED theo kết quả SMTP thực tế.
        Notification immediate = save(e, e.getType(), Instant.now(), NotificationStatus.PENDING);
        emailDeliveryService.deliver(
                immediate.getId(), e.getPatientEmail(), subject, templateBuilder.build(e));

        updateReminderSchedule(e);
    }

    private void updateReminderSchedule(AppointmentNotificationEvent e) {
        if (e.getType() == NotificationType.APPOINTMENT_CREATED) {
            scheduleReminder(e);
        }
        if (e.getType() == NotificationType.APPOINTMENT_RESCHEDULED) {
            cancelReminders(e.getAppointmentId());
            scheduleReminder(e);
        }
        if (e.getType() == NotificationType.APPOINTMENT_CANCELLED
                || e.getType() == NotificationType.APPOINTMENT_COMPLETED) {
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
        String reminderKey = reminderKey(e.getAppointmentId());
        Optional<Notification> existing = notificationRepository.findByReminderKey(reminderKey);
        if (existing.isPresent()) {
            updateReminder(existing.get(), e, remindAt);
            notificationRepository.save(existing.get());
            log.info("Da cap nhat lich nhac cho {} luc {}", e.getAppointmentId(), remindAt);
            return;
        }

        try {
            notificationRepository.saveAndFlush(buildNotification(
                    e, NotificationType.REMINDER, remindAt, NotificationStatus.PENDING));
            log.info("Da len lich nhac cho {} luc {}", e.getAppointmentId(), remindAt);
        } catch (DataIntegrityViolationException duplicateReminder) {
            if (notificationRepository.findByReminderKey(reminderKey).isEmpty()) {
                throw duplicateReminder;
            }
            log.info("Reminder cua lich {} da duoc instance khac tao", e.getAppointmentId());
        }
    }

    private void cancelReminders(String appointmentId) {
        if (appointmentId == null) return;
        List<Notification> active = notificationRepository.findByAppointmentIdAndTypeAndStatusIn(
                appointmentId,
                NotificationType.REMINDER,
                List.of(NotificationStatus.PENDING, NotificationStatus.PROCESSING));
        active.forEach(n -> {
            n.setStatus(NotificationStatus.CANCELLED);
            n.setClaimToken(null);
            n.setProcessingStartedAt(null);
        });
        notificationRepository.saveAll(active);
    }

    private Notification save(AppointmentNotificationEvent e, NotificationType type,
                              Instant scheduledAt, NotificationStatus status) {
        return notificationRepository.save(buildNotification(e, type, scheduledAt, status));
    }

    private Notification buildNotification(AppointmentNotificationEvent e, NotificationType type,
                                           Instant scheduledAt, NotificationStatus status) {
        return Notification.builder()
                .appointmentId(e.getAppointmentId())
                .recipientEmail(e.getPatientEmail())
                .type(type)
                .status(status)
                .scheduledAt(scheduledAt)
                .reminderKey(type == NotificationType.REMINDER
                        ? reminderKey(e.getAppointmentId()) : null)
                .sentAt(status == NotificationStatus.SENT ? Instant.now() : null)
                .appointmentTime(e.getAppointmentTime())
                .patientName(e.getPatientName())
                .doctorName(e.getDoctorName())
                .specialization(e.getSpecialization())
                .reason(e.getReason())
                .build();
    }

    private void updateReminder(Notification reminder, AppointmentNotificationEvent e, Instant remindAt) {
        reminder.setRecipientEmail(e.getPatientEmail());
        reminder.setScheduledAt(remindAt);
        reminder.setAppointmentTime(e.getAppointmentTime());
        reminder.setPatientName(e.getPatientName());
        reminder.setDoctorName(e.getDoctorName());
        reminder.setSpecialization(e.getSpecialization());
        reminder.setReason(e.getReason());
        reminder.setStatus(NotificationStatus.PENDING);
        reminder.setSentAt(null);
        reminder.setErrorMessage(null);
        reminder.setProcessingStartedAt(null);
        reminder.setClaimToken(null);
    }

    private String reminderKey(String appointmentId) {
        return "REMINDER:" + appointmentId;
    }

    public List<Notification> findAll(NotificationStatus status) {
        return status == null
                ? notificationRepository.findAllByOrderByScheduledAtDesc()
                : notificationRepository.findByStatusOrderByScheduledAtDesc(status);
    }
}
