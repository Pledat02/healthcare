package com.hehe.notification_service.entity;

import com.hehe.notification_service.dto.event.NotificationType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

/**
 * Nhat ky gui mail + hang doi nhac lich (US-08, US-09).
 *
 * Luu ban sao appointment_time / doctor_name lay tu event luc dat lich, de job
 * nhac 24h chay doc lap - khong phai goi appointment-service moi lan quet.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_status_scheduled", columnList = "status, scheduled_at"),
        @Index(name = "idx_notif_processing_started", columnList = "processing_started_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_notification_reminder_key", columnNames = "reminder_key")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    @Column(name = "appointment_id", nullable = false)
    String appointmentId;

    @Column(name = "recipient_email", nullable = false)
    String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    NotificationStatus status;

    /** Thoi diem can gui. Voi REMINDER = gio hen tru 24h; voi mail tuc thi = luc tao */
    @Column(name = "scheduled_at", nullable = false)
    Instant scheduledAt;

    /**
     * Chi REMINDER moi co gia tri. PostgreSQL cho phep nhieu NULL trong unique
     * constraint, nen cac mail tuc thi khong bi anh huong.
     */
    @Column(name = "reminder_key", length = 255)
    String reminderKey;

    /** Thong tin claim de worker khac co the thu hoi mot job bi treo. */
    @Column(name = "processing_started_at")
    Instant processingStartedAt;

    @Column(name = "claim_token", length = 36)
    String claimToken;

    @Builder.Default
    @Column(name = "attempt_count", nullable = false)
    int attemptCount = 0;

    // Ban sao tu event -> du de dung noi dung mail ma khong goi service khac
    @Column(name = "appointment_time")
    Instant appointmentTime;
    @Column(name = "patient_name")
    String patientName;
    @Column(name = "doctor_name")
    String doctorName;
    String specialization;
    String reason;

    @Column(name = "sent_at")
    Instant sentAt;

    @Column(name = "error_message", length = 500)
    String errorMessage;
}
