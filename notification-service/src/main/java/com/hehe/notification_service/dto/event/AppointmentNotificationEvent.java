package com.hehe.notification_service.dto.event;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
public class AppointmentNotificationEvent {
    NotificationType type;
    String patientName;
    String patientEmail;
    String doctorName;
    String specialization;
    Instant appointmentTime;
    String reason;
}
