package com.hehe.notification_service.repository;

import com.hehe.notification_service.dto.event.NotificationType;
import com.hehe.notification_service.entity.Notification;
import com.hehe.notification_service.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, String> {

    Optional<Notification> findByReminderKey(String reminderKey);

    /** Huy cac mail nhac dang cho khi lich hen bi huy */
    List<Notification> findByAppointmentIdAndTypeAndStatusIn(
            String appointmentId, NotificationType type, List<NotificationStatus> statuses);

    /**
     * Khoa cac dong den han trong transaction hien tai. SKIP LOCKED giup cac
     * instance khac bo qua dong da bi claim thay vi cung gui lai no.
     */
    @Query(value = """
            SELECT *
            FROM notifications
            WHERE type = 'REMINDER'
              AND (
                    (status = 'PENDING' AND scheduled_at <= :now)
                 OR (status = 'PROCESSING' AND processing_started_at <= :staleBefore)
              )
            ORDER BY scheduled_at
            FOR UPDATE SKIP LOCKED
            LIMIT :batchSize
            """, nativeQuery = true)
    List<Notification> lockDueReminders(
            @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore,
            @Param("batchSize") int batchSize);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Notification n
               SET n.status = com.hehe.notification_service.entity.NotificationStatus.SENT,
                   n.sentAt = :sentAt,
                   n.errorMessage = null,
                   n.processingStartedAt = null,
                   n.claimToken = null
             WHERE n.id = :id
               AND n.status = com.hehe.notification_service.entity.NotificationStatus.PROCESSING
               AND n.claimToken = :claimToken
            """)
    int markSent(@Param("id") String id,
                 @Param("claimToken") String claimToken,
                 @Param("sentAt") Instant sentAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Notification n
               SET n.status = com.hehe.notification_service.entity.NotificationStatus.FAILED,
                   n.errorMessage = :errorMessage,
                   n.processingStartedAt = null,
                   n.claimToken = null
             WHERE n.id = :id
               AND n.status = com.hehe.notification_service.entity.NotificationStatus.PROCESSING
               AND n.claimToken = :claimToken
            """)
    int markFailed(@Param("id") String id,
                   @Param("claimToken") String claimToken,
                   @Param("errorMessage") String errorMessage);

    List<Notification> findAllByOrderByScheduledAtDesc();

    List<Notification> findByStatusOrderByScheduledAtDesc(NotificationStatus status);
}
