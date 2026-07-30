package com.hehe.notification_service.consumer;

import com.hehe.notification_service.dto.event.AppointmentNotificationEvent;
import com.hehe.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer: nghe topic su kien lich hen tu Kafka roi giao cho NotificationService xu ly
 * (gui mail + len/huy lich nhac). Thay cho endpoint REST /api/notifications/appointments cu.
 * Kafka giu message -> notification chet roi song lai van xu ly tiep, khong mat mail.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentEventListener {

    private final NotificationService notificationService;

    @KafkaListener(topics = "${notification-topic}")
    public void onAppointmentEvent(AppointmentNotificationEvent event) {
        log.info("Nhan event Kafka: {} - lich {}", event.getType(), event.getAppointmentId());
        notificationService.handle(event);
    }
}
