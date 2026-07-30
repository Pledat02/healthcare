package com.hehe.patient_service.entity;

import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.validator.constraints.UniqueElements;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;

@Entity(name = "patients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Patient {
    @GeneratedValue(strategy = GenerationType.UUID)
            @Id
    @Column(updatable = false, nullable = false)
    String id;

    @Column(unique = true)
    String keycloakId;

    // us-02 Thiếu Họ tên hoặc SĐT → báo lỗi 400, không lưu.
    @Column(nullable = false)
    String fullName;
    @Column(nullable = false)
    String phone;

    Date dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Email
    String email;

    String address;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdDate;

    @UpdateTimestamp
    private Instant updatedDate;
}
