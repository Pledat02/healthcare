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
        @Index(name = "idx_notif_status_scheduled", columnList = "status, scheduledAt")
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

    @Column(nullable = false)
    String appointmentId;

    @Column(nullable = false)
    String recipientEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    NotificationStatus status;

    /** Thoi diem can gui. Voi REMINDER = gio hen tru 24h; voi mail tuc thi = luc tao */
    @Column(nullable = false)
    Instant scheduledAt;

    // Ban sao tu event -> du de dung noi dung mail ma khong goi service khac
    Instant appointmentTime;
    String patientName;
    String doctorName;
    String specialization;
    String reason;

    Instant sentAt;

    @Column(length = 500)
    String errorMessage;
}
