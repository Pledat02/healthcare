package com.hehe.notification_service.service;

import com.hehe.notification_service.dto.event.NotificationType;
import com.hehe.notification_service.entity.Notification;
import com.hehe.notification_service.entity.NotificationStatus;
import com.hehe.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderClaimServiceTest {

    @Mock NotificationRepository notificationRepository;
    @InjectMocks ReminderClaimService reminderClaimService;

    @Test
    void claimsPendingAndRecoversStaleProcessingReminder() {
        Instant now = Instant.parse("2026-08-20T02:00:00Z");
        Instant staleBefore = now.minus(20, ChronoUnit.MINUTES);
        Notification pending = reminder("pending", NotificationStatus.PENDING, 0);
        Notification stuck = reminder("stuck", NotificationStatus.PROCESSING, 1);

        when(notificationRepository.lockDueReminders(now, staleBefore, 50))
                .thenReturn(List.of(pending, stuck));

        List<ClaimedReminder> claimed = reminderClaimService
                .claimDueReminders(now, staleBefore, 50);

        assertThat(claimed).hasSize(2);
        assertThat(pending.getStatus()).isEqualTo(NotificationStatus.PROCESSING);
        assertThat(stuck.getStatus()).isEqualTo(NotificationStatus.PROCESSING);
        assertThat(pending.getAttemptCount()).isEqualTo(1);
        assertThat(stuck.getAttemptCount()).isEqualTo(2);
        assertThat(pending.getProcessingStartedAt()).isEqualTo(now);
        assertThat(claimed).extracting(ClaimedReminder::claimToken)
                .doesNotContainNull().doesNotHaveDuplicates();
    }

    private Notification reminder(String id, NotificationStatus status, int attempts) {
        return Notification.builder()
                .id(id)
                .appointmentId("appointment-" + id)
                .recipientEmail(id + "@example.com")
                .type(NotificationType.REMINDER)
                .status(status)
                .scheduledAt(Instant.parse("2026-08-20T01:00:00Z"))
                .attemptCount(attempts)
                .build();
    }
}
