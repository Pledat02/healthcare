package com.hehe.appointment_service.dto.request;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.sql.Timestamp;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAppointmentRequest {

    @NotNull
    String doctorId;
    @FutureOrPresent
    Instant appointmentTime;

    String reason;
    String status;
}
