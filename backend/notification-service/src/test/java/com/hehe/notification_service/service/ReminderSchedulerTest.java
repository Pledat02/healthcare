package com.hehe.notification_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReminderSchedulerTest {

    @Mock ReminderClaimService reminderClaimService;
    @Mock EmailService emailService;
    @Mock EmailTemplateBuilder templateBuilder;
    ReminderScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new ReminderScheduler(reminderClaimService, emailService, templateBuilder);
        ReflectionTestUtils.setField(scheduler, "claimTimeout", Duration.ofMinutes(20));
        ReflectionTestUtils.setField(scheduler, "batchSize", 50);
    }

    @Test
    void sameReminderClaimedOnceIsSentOnlyOnce() throws Exception {
        ClaimedReminder reminder = new ClaimedReminder(
                "reminder-1", "claim-1", "appointment-1", "patient@example.com",
                Instant.now().plusSeconds(3600), "Patient", "Doctor", "General", "Checkup");
        when(reminderClaimService.claimDueReminders(any(), any(), eq(50)))
                .thenReturn(List.of(reminder), List.of());
        when(templateBuilder.build(any())).thenReturn("<p>Reminder</p>");
        when(reminderClaimService.markSent(eq("reminder-1"), eq("claim-1"), any()))
                .thenReturn(true);

        scheduler.sendDueReminders();
        scheduler.sendDueReminders();

        verify(emailService, times(1)).sendHtml(
                "patient@example.com", "Nhắc lịch khám ngày mai", "<p>Reminder</p>");
        verify(reminderClaimService, times(1))
                .markSent(eq("reminder-1"), eq("claim-1"), any());
    }
}
