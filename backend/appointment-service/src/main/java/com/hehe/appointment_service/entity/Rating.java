package com.hehe.appointment_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * Danh gia cua benh nhan sau khi lich hen COMPLETED.
 * Unique appointment_id: moi lich chi danh gia 1 lan.
 */
@Getter
@Setter
@Entity
@Table(name = "ratings",
        uniqueConstraints = @UniqueConstraint(name = "uq_rating_appointment", columnNames = "appointment_id"))
public class Rating {
    @GeneratedValue(strategy = GenerationType.UUID)
    @Id
    String id;

    @Column(name = "appointment_id", nullable = false)
    String appointmentId;

    @Column(name = "doctor_id", nullable = false)
    String doctorId;

    @Column(name = "patient_id", nullable = false)
    String patientId;

    @Column(nullable = false)
    int stars;

    @Column(length = 1000)
    String comment;

    @CreationTimestamp
    Instant createdAt;
}
