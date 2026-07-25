package com.hehe.appointment_service.dto.response;

import com.hehe.appointment_service.utils.AppointmentStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.sql.Timestamp;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {
    @NotNull
    DoctorDto doctor;
    @NotNull
    PatientDto patient;

    @FutureOrPresent
    Timestamp appointmentTime;

    String reason;
    AppointmentStatus status;
}
