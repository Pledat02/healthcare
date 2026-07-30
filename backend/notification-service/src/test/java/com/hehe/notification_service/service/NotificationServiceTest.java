package com.hehe.notification_service.service;

import com.hehe.notification_service.dto.event.AppointmentNotificationEvent;
import com.hehe.notification_service.dto.event.NotificationType;
import com.hehe.notification_service.entity.Notification;
import com.hehe.notification_service.entity.NotificationStatus;
import com.hehe.notification_service.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock EmailDeliveryService emailDeliveryService;
    @Mock EmailTemplateBuilder templateBuilder;
    @Mock NotificationRepository notificationRepository;
    @InjectMocks NotificationService notificationService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(notificationService, "reminderHoursBefore", 24);
    }

    @Test
    void createdAppointmentQueuesImmediateEmailAndReminder() {
        when(templateBuilder.build(any())).thenReturn("<p>mail</p>");
        when(notificationRepository.findByReminderKey("REMINDER:appointment-1"))
                .thenReturn(Optional.empty());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            if (notification.getId() == null) notification.setId("mail-" + notification.getType());
            return notification;
        });
        when(notificationRepository.saveAndFlush(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            notification.setId("mail-REMINDER");
            return notification;
        });

        notificationService.handle(event("patient@example.com"));

        ArgumentCaptor<Notification> immediateCaptor = ArgumentCaptor.forClass(Notification.class);
        ArgumentCaptor<Notification> reminderCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(immediateCaptor.capture());
        verify(notificationRepository).saveAndFlush(reminderCaptor.capture());
        assertThat(immediateCaptor.getValue().getType()).isEqualTo(NotificationType.APPOINTMENT_CREATED);
        assertThat(reminderCaptor.getValue().getType()).isEqualTo(NotificationType.REMINDER);
        assertThat(reminderCaptor.getValue().getReminderKey()).isEqualTo("REMINDER:appointment-1");
        assertThat(reminderCaptor.getValue().getStatus()).isEqualTo(NotificationStatus.PENDING);
        verify(emailDeliveryService).deliver(
                "mail-APPOINTMENT_CREATED", "patient@example.com",
                "Đặt lịch khám thành công", "<p>mail</p>");
    }

    @Test
    void missingRecipientDoesNotCreateBrokenNotification() {
        notificationService.handle(event(" "));

        verify(notificationRepository, never()).save(any());
        verify(emailDeliveryService, never()).deliver(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void appointmentWithin24hDoesNotScheduleReminder() {
        when(templateBuilder.build(any())).thenReturn("<p>mail</p>");
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            if (n.getId() == null) n.setId("mail");
            return n;
        });

        // Lich dien ra sau 1h (< 24h) -> khong con kip nhac truoc 24h
        notificationService.handle(customEvent(NotificationType.APPOINTMENT_CREATED,
                "patient@example.com", Instant.now().plus(1, ChronoUnit.HOURS)));

        // Chi 1 ban ghi mail xac nhan, KHONG tao REMINDER
        verify(notificationRepository, times(1)).save(any());
        verify(notificationRepository, never()).findByReminderKey(anyString());
    }

    @Test
    void cancelledAppointmentCancelsPendingReminders() {
        when(templateBuilder.build(any())).thenReturn("<p>mail</p>");
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            if (n.getId() == null) n.setId("mail");
            return n;
        });
        Notification pending = Notification.builder()
                .type(NotificationType.REMINDER).status(NotificationStatus.PENDING).build();
        when(notificationRepository.findByAppointmentIdAndTypeAndStatusIn(
                "appointment-1", NotificationType.REMINDER,
                List.of(NotificationStatus.PENDING, NotificationStatus.PROCESSING)))
                .thenReturn(List.of(pending));

        notificationService.handle(customEvent(NotificationType.APPOINTMENT_CANCELLED,
                "patient@example.com", Instant.now().plus(48, ChronoUnit.HOURS)));

        // Lich bi huy -> mail nhac dang cho chuyen sang CANCELLED
        assertThat(pending.getStatus()).isEqualTo(NotificationStatus.CANCELLED);
        verify(notificationRepository).saveAll(any());
    }

    private AppointmentNotificationEvent customEvent(NotificationType type, String email, Instant time) {
        return AppointmentNotificationEvent.builder()
                .type(type).appointmentId("appointment-1").patientName("Nguyễn An")
                .patientEmail(email).doctorName("BS. Minh").specialization("Nội tổng quát")
                .appointmentTime(time).reason("Tái khám").build();
    }

    private AppointmentNotificationEvent event(String email) {
        return AppointmentNotificationEvent.builder()
                .type(NotificationType.APPOINTMENT_CREATED)
                .appointmentId("appointment-1")
                .patientName("Nguyễn An")
                .patientEmail(email)
                .doctorName("BS. Minh")
                .specialization("Nội tổng quát")
                .appointmentTime(Instant.now().plus(48, ChronoUnit.HOURS))
                .reason("Tái khám")
                .build();
    }
}
