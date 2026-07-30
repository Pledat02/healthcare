package com.hehe.appointment_service.client;

import com.hehe.appointment_service.dto.event.AppointmentNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Ban su kien lich hen sang Kafka (bat dong bo) thay cho goi REST dong bo.
 * appointment-service khong con phu thuoc notification-service dang song hay chet:
 * Kafka giu message, notification xu ly sau khi song lai.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationClient {

    private final KafkaTemplate<String, Object> kafkaTemplate;   // khai o KafkaProducerConfig

    @Value("${notification-topic}")
    private String topic;

    public void send(AppointmentNotificationEvent event) {
        try {
            // Key = appointmentId -> cac su kien cua cung 1 lich vao cung partition (giu thu tu)
            kafkaTemplate.send(topic, event.getAppointmentId(), event);
        } catch (Exception e) {
            // PRD muc 6: notification loi KHONG duoc lam hong dat lich -> chi log
            log.error("Ban event notification sang Kafka that bai: {}", e.getMessage());
        }
    }
}
