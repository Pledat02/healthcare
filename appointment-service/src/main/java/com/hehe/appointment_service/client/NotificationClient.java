package com.hehe.appointment_service.client;

import com.hehe.appointment_service.dto.event.AppointmentNotificationEvent;
import com.hehe.appointment_service.utils.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationClient {

    private final RestClient notificationRestClient;

    public void sendAppointmentCreated(AppointmentNotificationEvent event) {
        try {
            notificationRestClient.post()
                    .uri("/api/notifications/appointment-created")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + SecurityUtils.currentToken())
                    .body(event)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // PRD mục 6: notification lỗi KHÔNG được làm hỏng đặt lịch -> chỉ log
            log.error("Gửi event notification thất bại: {}", e.getMessage());
        }
    }
}
