package com.hehe.notification_service.service;

import com.hehe.notification_service.dto.event.AppointmentNotificationEvent;
import com.hehe.notification_service.dto.event.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class ReminderScheduler {

    private final ReminderClaimService reminderClaimService;
    private final EmailService emailService;
    private final EmailTemplateBuilder templateBuilder;

    @Value("${notification.claim-timeout:PT20M}")
    private Duration claimTimeout;

    @Value("${notification.batch-size:50}")
    private int batchSize;

    @Scheduled(cron = "${notification.scan-cron}")
    @SchedulerLock(
            name = "notification-send-due-reminders",
            lockAtMostFor = "${notification.scheduler-lock-at-most:PT15M}",
            lockAtLeastFor = "${notification.scheduler-lock-at-least:PT5S}")
    public void sendDueReminders() {
        Instant now = Instant.now();
        List<ClaimedReminder> due = reminderClaimService.claimDueReminders(
                now, now.minus(claimTimeout), batchSize);

        if (due.isEmpty()) return;
        log.info("Co {} mail nhac lich den han", due.size());

        for (ClaimedReminder reminder : due) {
            try {
                emailService.sendHtml(
                        reminder.recipientEmail(),
                        "Nhắc lịch khám ngày mai",
                        templateBuilder.build(toEvent(reminder)));
                if (!reminderClaimService.markSent(
                        reminder.id(), reminder.claimToken(), Instant.now())) {
                    log.warn("Reminder {} khong con thuoc claim nay", reminder.id());
                }
            } catch (Exception ex) {
                reminderClaimService.markFailed(
                        reminder.id(), reminder.claimToken(), ex.getMessage());
                log.error("Gui mail nhac lich {} that bai: {}", reminder.id(), ex.getMessage());
            }
        }
    }

    private AppointmentNotificationEvent toEvent(ClaimedReminder reminder) {
        return AppointmentNotificationEvent.builder()
                .type(NotificationType.REMINDER)
                .appointmentId(reminder.appointmentId())
                .patientName(reminder.patientName())
                .patientEmail(reminder.recipientEmail())
                .doctorName(reminder.doctorName())
                .specialization(reminder.specialization())
                .appointmentTime(reminder.appointmentTime())
                .reason(reminder.reason())
                .build();
    }
}
