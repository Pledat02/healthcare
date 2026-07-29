package com.hehe.appointment_service.dto.response;

import com.hehe.appointment_service.utils.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {
    String id;
    String patientId;
    String doctorId;
    Instant appointmentTime;
    int durationMinutes;
    String reason;
    AppointmentStatus status;
}
