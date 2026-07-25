package com.hehe.medical_record_service.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "medical_records")
public class MedicalRecord {
    @GeneratedValue(strategy = GenerationType.UUID)
    @Id
    String id;

    @Column(unique = true, nullable = false)
    String appointmentId;

    @Column(nullable = false) String patientId;

    @Column(nullable = false) String doctorId;

    @Column(columnDefinition = "TEXT", nullable = false) String diagnosis;

    @Column(columnDefinition = "TEXT") String notes;

    @CreationTimestamp
    @Column(updatable = false)
    Instant createdAt;

    @OneToMany(mappedBy = "medicalRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    List<PrescriptionItem> prescriptionItems;

}
