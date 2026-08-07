package com.hehe.appointment_service.controller;

import com.hehe.appointment_service.dto.request.CancelAppointmentRequest;
import com.hehe.appointment_service.dto.request.CreationAppointmentRequest;
import com.hehe.appointment_service.dto.request.RateRequest;
import com.hehe.appointment_service.dto.request.UpdateAppointmentRequest;
import com.hehe.appointment_service.dto.response.ApiResponse;
import com.hehe.appointment_service.dto.response.AppointmentResponse;
import com.hehe.appointment_service.dto.response.AppointmentStatisticsResponse;
import com.hehe.appointment_service.dto.response.PageResponse;
import com.hehe.appointment_service.dto.response.RatingResponse;
import com.hehe.appointment_service.service.AppointmentService;
import com.hehe.appointment_service.service.AppointmentStatisticsService;
import com.hehe.appointment_service.utils.AppointmentStatus;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AppointmentController {

    AppointmentService appointmentService;
    AppointmentStatisticsService appointmentStatisticsService;

    // US-05: benh nhan dat lich
    @PostMapping
    public ApiResponse<AppointmentResponse> create(@Valid @RequestBody CreationAppointmentRequest request) {
        return ApiResponse.<AppointmentResponse>builder()
                .code(201)
                .data(appointmentService.create(request))
                .message("Đặt lịch hẹn thành công")
                .build();
    }

    // Doi gio hen / sua ly do (chu lich)
    @PutMapping("/{id}")
    public ApiResponse<AppointmentResponse> update(@PathVariable String id,
                                                   @Valid @RequestBody UpdateAppointmentRequest request) {
        return ApiResponse.<AppointmentResponse>builder()
                .code(200)
                .data(appointmentService.update(id, request))
                .message("Cập nhật lịch hẹn thành công")
                .build();
    }

    // US-06: benh nhan huy lich; ADMIN huy bat ky lich nao (kem ly do -> email)
    @PatchMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable String id,
                                    @RequestBody(required = false) CancelAppointmentRequest body) {
        appointmentService.cancel(id, body == null ? null : body.getReason());
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Hủy lịch hẹn thành công")
                .build();
    }

    // Bac si / admin xac nhan lich
    @PatchMapping("/{id}/confirm")
    public ApiResponse<Void> confirm(@PathVariable String id) {
        appointmentService.confirm(id);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Xác nhận lịch hẹn thành công")
                .build();
    }

    // Bac si danh dau da kham xong
    @PatchMapping("/{id}/complete")
    public ApiResponse<Void> complete(@PathVariable String id) {
        appointmentService.complete(id);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Đánh dấu hoàn thành lịch hẹn thành công")
                .build();
    }

    // Benh nhan danh gia bac si (sau khi lich COMPLETED)
    @PostMapping("/{id}/rate")
    public ApiResponse<Void> rate(@PathVariable String id, @Valid @RequestBody RateRequest request) {
        appointmentService.rate(id, request);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Cảm ơn bạn đã đánh giá")
                .build();
    }

    // Danh sach nhan xet cua 1 bac si (ai da dang nhap cung xem duoc)
    @GetMapping("/doctors/{doctorId}/ratings")
    public ApiResponse<List<RatingResponse>> getDoctorRatings(@PathVariable String doctorId) {
        return ApiResponse.<List<RatingResponse>>builder()
                .code(200)
                .data(appointmentService.getDoctorRatings(doctorId))
                .message("Lấy danh sách đánh giá thành công")
                .build();
    }

    // Benh nhan xem lich cua minh
    @GetMapping("/patients/me")
    public ApiResponse<List<AppointmentResponse>> getMyPatientAppointments() {
        return ApiResponse.<List<AppointmentResponse>>builder()
                .code(200)
                .data(appointmentService.getMyPatientAppointments())
                .message("Lấy danh sách lịch hẹn thành công")
                .build();
    }

    // US-07: bac si xem lich cua minh. Theo NGAY (date) hoac theo KHOANG (from+to, vd 1 tuan).
    @GetMapping("/doctors/me")
    public ApiResponse<List<AppointmentResponse>> getMyDoctorAppointments(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<AppointmentResponse> data = (from != null && to != null)
                ? appointmentService.getMyDoctorAppointmentsRange(from, to)
                : appointmentService.getMyDoctorAppointments(date != null ? date : LocalDate.now());
        return ApiResponse.<List<AppointmentResponse>>builder()
                .code(200)
                .data(data)
                .message("Lấy danh sách lịch hẹn thành công")
                .build();
    }

    // Bac si xem LICH SU hen (cac lich da qua), phan trang moi nhat truoc
    @GetMapping("/doctors/me/history")
    public ApiResponse<PageResponse<AppointmentResponse>> getMyDoctorHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.<PageResponse<AppointmentResponse>>builder()
                .code(200)
                .data(appointmentService.getMyDoctorHistory(page, size))
                .message("Lấy lịch sử hẹn thành công")
                .build();
    }

    // Dashboard ADMIN: tong hop tai backend, chi tra top ID de frontend batch-load ten.
    @GetMapping("/statistics")
    public ApiResponse<AppointmentStatisticsResponse> statistics(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.<AppointmentStatisticsResponse>builder()
                .code(200)
                .data(appointmentStatisticsService.getStatistics(from, to, limit))
                .message("Lấy thống kê lịch hẹn thành công")
                .build();
    }

    // Cac gio da co lich (chua huy) cua 1 bac si trong ngay -> FE lam mo slot da dat khi dat lich.
    @GetMapping("/doctors/{doctorId}/booked")
    public ApiResponse<List<Instant>> getBookedTimes(
            @PathVariable String doctorId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ApiResponse.<List<Instant>>builder()
                .code(200)
                .data(appointmentService.getBookedTimes(doctorId, date))
                .message("Lấy khung giờ đã đặt thành công")
                .build();
    }

    // Xem chi tiet 1 lich hen
    @GetMapping("/{id}")
    public ApiResponse<AppointmentResponse> getOne(@PathVariable String id) {
        return ApiResponse.<AppointmentResponse>builder()
                .code(200)
                .data(appointmentService.getOne(id))
                .message("Lấy thông tin lịch hẹn thành công")
                .build();
    }

    // ADMIN xem toan bo lich hen - PHAN TRANG (page/size toi da 100) + loc trang thai
    @GetMapping
    public ApiResponse<PageResponse<AppointmentResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) AppointmentStatus status) {
        return ApiResponse.<PageResponse<AppointmentResponse>>builder()
                .code(200)
                .data(appointmentService.getAll(page, size, status))
                .message("Lấy danh sách lịch hẹn thành công")
                .build();
    }
}
