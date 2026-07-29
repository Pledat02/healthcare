package com.hehe.patient_service.controller;

import com.hehe.patient_service.dto.request.CreationPatientRequest;
import com.hehe.patient_service.dto.request.UpdationPatientRequest;
import com.hehe.patient_service.dto.response.ApiResponse;
import com.hehe.patient_service.dto.response.PatientResponse;
import com.hehe.patient_service.service.PatientService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Builder
@RequestMapping("/api/patients")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PatientController {
    PatientService patientService;

    @PostMapping("/")
    public ApiResponse<PatientResponse> create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody
                                               CreationPatientRequest request) {
        String keycloakId = jwt.getSubject();
        return ApiResponse.<PatientResponse>builder().
                code(201)
                .data(patientService.create(request,keycloakId))
                .message("Đã tạo thành công bệnh nhân")
                .build();
    }

    // Batch: lay nhieu benh nhan theo id (fix N+1 o trang lich hen admin/bac si)
    @GetMapping("/batch")
    public ApiResponse<List<PatientResponse>> getByIds(@RequestParam List<String> ids) {
        return ApiResponse.<List<PatientResponse>>builder()
                .code(200)
                .data(patientService.getByIds(ids))
                .message("Lấy bệnh nhân theo id thành công")
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<PatientResponse> getOne( @PathVariable
                                               String id) {
        return ApiResponse.<PatientResponse>builder().
                code(200)
                .data(patientService.getOne(id))
                .message("Đã lấy thành công bệnh nhân")
                .build();
    }
    @GetMapping("/me")
    public ApiResponse<PatientResponse> getMe( ) {
        return ApiResponse.<PatientResponse>builder().
                code(200)
                .data(patientService.getMe())
                .message("Đã lấy thành công bệnh nhân")
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<PatientResponse> update( @PathVariable String id, @Valid @RequestBody UpdationPatientRequest request) {
        return ApiResponse.<PatientResponse>builder()
                .code(200)
                .data(patientService.update(id, request))
                .message("Cập nhật thành công bệnh nhân")
                .build();
    }
    @DeleteMapping("/{id}")
    public  ApiResponse<Void> delete(@PathVariable String id){
        patientService.delete(id);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("xóa thành công bệnh nhân")
                .build();

    }
    @GetMapping
    public ApiResponse<List<PatientResponse>> getAll(){
        return ApiResponse.<List<PatientResponse>>builder()
                .code(200)
                .data(patientService.getAll())
                .message("lấy thành công tất cả bệnh nhân")
                .build();
    }


}
