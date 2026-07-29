package com.hehe.notification_service.dto.event;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentNotificationEvent {
    NotificationType type;
    String appointmentId;   // de len lich nhac va huy nhac khi lich bi huy
    String patientName;
    String patientEmail;
    String doctorName;
    String specialization;
    Instant appointmentTime;
    String reason;
}
