package com.hehe.medical_record_service.controller;

import com.hehe.medical_record_service.dto.request.CreationMedicalRecordRequest;
import com.hehe.medical_record_service.dto.response.ApiResponse;
import com.hehe.medical_record_service.dto.response.MedicalRecordResponse;
import com.hehe.medical_record_service.service.MedicalRecordService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicalRecordController {

    MedicalRecordService medicalRecordService;

    // US-10: bac si ghi ho so kham (chan doan + don thuoc)
    @PostMapping
    public ApiResponse<MedicalRecordResponse> create(@Valid @RequestBody CreationMedicalRecordRequest request) {
        return ApiResponse.<MedicalRecordResponse>builder()
                .code(201)
                .data(medicalRecordService.create(request))
                .message("Ghi hồ sơ khám thành công")
                .build();
    }

    // US-11: benh nhan xem lich su kham cua minh
    @GetMapping("/me")
    public ApiResponse<List<MedicalRecordResponse>> getMyHistory() {
        return ApiResponse.<List<MedicalRecordResponse>>builder()
                .code(200)
                .data(medicalRecordService.getMyHistoryPatientMedicalRecord())
                .message("Lấy lịch sử khám thành công")
                .build();
    }

    // G4: bac si / admin xem lich su kham cua 1 benh nhan
    @GetMapping("/patients/{patientId}")
    public ApiResponse<List<MedicalRecordResponse>> getPatientHistory(@PathVariable String patientId) {
        return ApiResponse.<List<MedicalRecordResponse>>builder()
                .code(200)
                .data(medicalRecordService.getHistoryPatientMedicalRecord(patientId))
                .message("Lấy lịch sử khám của bệnh nhân thành công")
                .build();
    }

    // Ho so kham cua 1 lich hen
    @GetMapping("/appointments/{appointmentId}")
    public ApiResponse<MedicalRecordResponse> getByAppointment(@PathVariable String appointmentId) {
        return ApiResponse.<MedicalRecordResponse>builder()
                .code(200)
                .data(medicalRecordService.getByAppointmentId(appointmentId))
                .message("Lấy hồ sơ khám thành công")
                .build();
    }

    // Chi tiet 1 ho so kham theo id
    @GetMapping("/{id}")
    public ApiResponse<MedicalRecordResponse> getOne(@PathVariable String id) {
        return ApiResponse.<MedicalRecordResponse>builder()
                .code(200)
                .data(medicalRecordService.getOne(id))
                .message("Lấy hồ sơ khám thành công")
                .build();
    }
}
