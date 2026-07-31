package com.hehe.doctor_service.controller;

import com.hehe.doctor_service.dto.request.CreateLeaveRequest;
import com.hehe.doctor_service.dto.response.ApiResponse;
import com.hehe.doctor_service.dto.response.DoctorLeaveResponse;
import com.hehe.doctor_service.service.DoctorLeaveService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Nghi phep bac si.
 *  - /me/leaves: bac si tu quan ly ngay nghi cua chinh minh (POST/GET/DELETE)
 *  - /{doctorId}/leaves: ai da dang nhap cung xem duoc (benh nhan can de an slot khi dat lich)
 */
@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class DoctorLeaveController {

    DoctorLeaveService leaveService;

    @PostMapping("/me/leaves")
    public ApiResponse<DoctorLeaveResponse> addMyLeave(@Valid @RequestBody CreateLeaveRequest request) {
        return ApiResponse.<DoctorLeaveResponse>builder()
                .data(leaveService.addMyLeave(request))
                .code(201)
                .message("Đăng ký ngày nghỉ thành công")
                .build();
    }

    @GetMapping("/me/leaves")
    public ApiResponse<List<DoctorLeaveResponse>> getMyLeaves() {
        return ApiResponse.<List<DoctorLeaveResponse>>builder()
                .data(leaveService.getMyLeaves())
                .code(200)
                .message("Lấy danh sách ngày nghỉ thành công")
                .build();
    }

    @DeleteMapping("/me/leaves/{leaveId}")
    public ApiResponse<Void> deleteMyLeave(@PathVariable String leaveId) {
        leaveService.deleteMyLeave(leaveId);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Đã xóa ngày nghỉ")
                .build();
    }

    // Ngay nghi sap toi cua 1 bac si (cho benh nhan xem khi dat lich)
    @GetMapping("/{doctorId}/leaves")
    public ApiResponse<List<DoctorLeaveResponse>> getDoctorLeaves(@PathVariable String doctorId) {
        return ApiResponse.<List<DoctorLeaveResponse>>builder()
                .data(leaveService.getUpcoming(doctorId))
                .code(200)
                .message("Lấy ngày nghỉ của bác sĩ thành công")
                .build();
    }
}
