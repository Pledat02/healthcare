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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
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
        when(notificationRepository.existsByAppointmentIdAndTypeAndStatus(
                "appointment-1", NotificationType.REMINDER, NotificationStatus.PENDING))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification notification = invocation.getArgument(0);
            if (notification.getId() == null) notification.setId("mail-" + notification.getType());
            return notification;
        });

        notificationService.handle(event("patient@example.com"));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        List<Notification> saved = captor.getAllValues();
        assertThat(saved).extracting(Notification::getType)
                .containsExactly(NotificationType.APPOINTMENT_CREATED, NotificationType.REMINDER);
        assertThat(saved).extracting(Notification::getStatus)
                .containsOnly(NotificationStatus.PENDING);
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
