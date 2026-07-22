package com.hehe.doctor_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Doctor {
    @GeneratedValue(strategy = GenerationType.UUID)
    @Id
    String id;

    String keycloakId;

    String fullName;

    String phone;

    String email;

    LocalTime workStartTime;
    LocalTime workEndTime;

    String specialization;

    @CreationTimestamp
    Instant createdAt;

    @UpdateTimestamp
    Instant updatedTime;
}
