package com.hehe.doctor_service.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Ngay nghi (ca ngay) cua bac si. Slot picker an het slot trong ngay nay;
 * appointment-service tu choi dat/doi lich trung ngay nghi.
 * Unique (doctor_id, leave_date): moi bac si chi 1 ban ghi/ngay.
 */
@Entity
@Table(name = "doctor_leaves",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_doctor_leave_date", columnNames = {"doctor_id", "leave_date"}))
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DoctorLeave {

    @GeneratedValue(strategy = GenerationType.UUID)
    @Id
    String id;

    @Column(name = "doctor_id", nullable = false)
    String doctorId;

    @Column(name = "leave_date", nullable = false)
    LocalDate leaveDate;

    String reason;

    @CreationTimestamp
    Instant createdAt;
}
