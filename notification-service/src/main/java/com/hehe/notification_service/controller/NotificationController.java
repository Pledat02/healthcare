package com.hehe.notification_service.controller;

import com.hehe.notification_service.dto.event.AppointmentNotificationEvent;
import com.hehe.notification_service.dto.response.ApiResponse;
import com.hehe.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/appointments")
    public ApiResponse<Void> onAppointmentEvent(@RequestBody AppointmentNotificationEvent event) {
        notificationService.handle(event);
        return ApiResponse.<Void>builder().code(200).message("Đã nhận").build();
    }
}
