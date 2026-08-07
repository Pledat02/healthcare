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
    String patientName;   // lam giau tu patient-service (batch, service-account) cho trang bac si/admin
    String patientPhone;
    Instant appointmentTime;
    int durationMinutes;
    String reason;
    AppointmentStatus status;
    boolean rated;   // benh nhan da danh gia lich nay chua (chi set o danh sach cua benh nhan)
    String doctorName;      // lam giau tu doctor-service (batch) -> FE khoi goi /doctors/batch
    String specialization;
    String cancelReason;    // ly do huy (neu lich bi ADMIN huy)
}
