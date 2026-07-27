package com.hehe.notification_service.repository;

import com.hehe.notification_service.dto.event.NotificationType;
import com.hehe.notification_service.entity.Notification;
import com.hehe.notification_service.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    /** US-09: cac mail den han gui ma chua gui */
    List<Notification> findByStatusAndScheduledAtLessThanEqual(NotificationStatus status, Instant now);

    /** Tranh len lich nhac 2 lan cho cung 1 lich hen */
    boolean existsByAppointmentIdAndType(String appointmentId, NotificationType type);

    /** Huy cac mail nhac dang cho khi lich hen bi huy */
    List<Notification> findByAppointmentIdAndTypeAndStatus(
            String appointmentId, NotificationType type, NotificationStatus status);

    List<Notification> findAllByOrderByScheduledAtDesc();

    List<Notification> findByStatusOrderByScheduledAtDesc(NotificationStatus status);
}
