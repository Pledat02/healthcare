package com.hehe.doctor_service.controller;

import com.hehe.doctor_service.dto.request.CreationDoctorRequest;
import com.hehe.doctor_service.dto.request.UpdateDoctorRequest;
import com.hehe.doctor_service.dto.response.ApiResponse;
import com.hehe.doctor_service.dto.response.DoctorResponse;
import com.hehe.doctor_service.service.DoctorService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
public class DoctorController {
    DoctorService doctorService;

    @GetMapping("/{id}")
    public ApiResponse<DoctorResponse> getOne(@PathVariable String id){
        return ApiResponse.<DoctorResponse>builder()
                .data(doctorService.getOne(id))
                .code(200)
                .message("Lấy thông tin bác sĩ thành công")
                .build();
    }
    @GetMapping("/me")
    public ApiResponse<DoctorResponse> getMe(){
        return ApiResponse.<DoctorResponse>builder()
                .data(doctorService.getMe())
                .code(200)
                .message("Lấy thông tin bác sĩ thành công")
                .build();
    }
    @PostMapping()
    public ApiResponse<DoctorResponse> create(@Valid @RequestBody CreationDoctorRequest request){
        return ApiResponse.<DoctorResponse>builder()
                .data(doctorService.create(request))
                .code(201)
                .message("Thêm bác sĩ thành công")
                .build();
    }
    @GetMapping()
    public ApiResponse<List<DoctorResponse>> getAll(){
        return ApiResponse.<List<DoctorResponse>>builder()
                .data(doctorService.getAll())
                .code(200)
                .message("Lấy danh sách bác sĩ thành công")
                .build();
    }
    @PutMapping("/{id}")
    public ApiResponse<DoctorResponse> update(@PathVariable String id,
                                              @Valid @RequestBody UpdateDoctorRequest request){
        return ApiResponse.<DoctorResponse>builder()
                .data(doctorService.update(id,request))
                .code(200)
                .message("Cập nhật bác sĩ thành công")
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id){
        doctorService.delete(id);
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Xóa bác sĩ thành công")
                .build();
    }
}
