package com.hehe.appointment_service.entity;

import com.hehe.appointment_service.utils.AppointmentStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.FutureOrPresent;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.sql.Timestamp;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "appointments")
public class Appointment {
    @GeneratedValue(strategy = GenerationType.UUID)
    @Id
    String id;
    String patientId;
    String doctorId;

    Instant appointmentTime;
    int durationMinutes;
    String reason;
    String cancelReason;   // ly do huy (admin nhap) -> gui kem email cho benh nhan

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    AppointmentStatus status = AppointmentStatus.PENDING;

    @CreationTimestamp
    Instant createdAt;
    @UpdateTimestamp
    Instant  updatedAt;
}
