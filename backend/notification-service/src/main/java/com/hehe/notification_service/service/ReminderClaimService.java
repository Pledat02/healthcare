package com.hehe.notification_service.service;

import com.hehe.notification_service.entity.Notification;
import com.hehe.notification_service.entity.NotificationStatus;
import com.hehe.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReminderClaimService {

    private final NotificationRepository notificationRepository;

    /** Khoa dong va doi sang PROCESSING trong cung mot transaction. */
    @Transactional
    public List<ClaimedReminder> claimDueReminders(Instant now, Instant staleBefore, int batchSize) {
        List<Notification> claimed = notificationRepository
                .lockDueReminders(now, staleBefore, batchSize);

        long recovered = claimed.stream()
                .filter(n -> n.getStatus() == NotificationStatus.PROCESSING)
                .count();

        List<ClaimedReminder> result = claimed.stream().map(n -> {
            String claimToken = UUID.randomUUID().toString();
            n.setStatus(NotificationStatus.PROCESSING);
            n.setProcessingStartedAt(now);
            n.setClaimToken(claimToken);
            n.setAttemptCount(n.getAttemptCount() + 1);
            n.setErrorMessage(null);
            return snapshot(n, claimToken);
        }).toList();

        if (recovered > 0) {
            log.warn("Da thu hoi {} reminder PROCESSING bi treo", recovered);
        }
        return result;
    }

    @Transactional
    public boolean markSent(String id, String claimToken, Instant sentAt) {
        return notificationRepository.markSent(id, claimToken, sentAt) == 1;
    }

    @Transactional
    public boolean markFailed(String id, String claimToken, String errorMessage) {
        return notificationRepository.markFailed(id, claimToken, truncate(errorMessage)) == 1;
    }

    private ClaimedReminder snapshot(Notification n, String claimToken) {
        return new ClaimedReminder(
                n.getId(), claimToken, n.getAppointmentId(), n.getRecipientEmail(),
                n.getAppointmentTime(), n.getPatientName(), n.getDoctorName(),
                n.getSpecialization(), n.getReason());
    }

    private String truncate(String value) {
        if (value == null) return null;
        return value.length() > 500 ? value.substring(0, 500) : value;
    }
}
