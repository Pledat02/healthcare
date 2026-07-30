package com.hehe.notification_service.service;

import java.time.Instant;

/** Ban sao bat bien cua reminder sau khi transaction claim da commit. */
public record ClaimedReminder(
        String id,
        String claimToken,
        String appointmentId,
        String recipientEmail,
        Instant appointmentTime,
        String patientName,
        String doctorName,
        String specialization,
        String reason) {
}
