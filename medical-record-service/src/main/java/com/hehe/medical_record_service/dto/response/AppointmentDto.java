package com.hehe.medical_record_service.dto.response;

import com.hehe.medical_record_service.utils.AppointmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentDto {
    String id;
    String patientId;
    String doctorId;
    Instant appointmentTime;
    int durationMinutes;
    String reason;
    AppointmentStatus status;
}
