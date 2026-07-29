package com.hehe.notification_service.controller;

import com.hehe.notification_service.dto.event.AppointmentNotificationEvent;
import com.hehe.notification_service.dto.response.ApiResponse;
import com.hehe.notification_service.entity.Notification;
import com.hehe.notification_service.entity.NotificationStatus;
import com.hehe.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // Nhan su kien tu appointment-service (noi bo)
    @PostMapping("/appointments")
    public ApiResponse<Void> onAppointmentEvent(@RequestBody AppointmentNotificationEvent event) {
        notificationService.handle(event);
        return ApiResponse.<Void>builder().code(200).message("Đã nhận").build();
    }

    // ADMIN xem nhat ky gui mail, loc theo trang thai
    @GetMapping
    public ApiResponse<List<Notification>> list(@RequestParam(required = false) NotificationStatus status) {
        return ApiResponse.<List<Notification>>builder()
                .code(200)
                .data(notificationService.findAll(status))
                .message("Lấy nhật ký thông báo thành công")
                .build();
    }
}
