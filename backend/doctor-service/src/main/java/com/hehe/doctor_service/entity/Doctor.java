package com.hehe.doctor_service.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Doctor {
    @GeneratedValue(strategy = GenerationType.UUID)
    @Id
    String id;

    @Column(unique = true)
    String keycloakId;

    String fullName;

    String phone;

    String email;

    LocalTime workStartTime;
    LocalTime workEndTime;

    String specialization;

    // Danh gia: luu tong sao + so luot -> tinh trung binh. @ColumnDefault de row cu = 0.
    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("0")
    Integer ratingSum = 0;

    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("0")
    Integer ratingCount = 0;

    @CreationTimestamp
    Instant createdAt;

    @UpdateTimestamp
    Instant updatedTime;
}
