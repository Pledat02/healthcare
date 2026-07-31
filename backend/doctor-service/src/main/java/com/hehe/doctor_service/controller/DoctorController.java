package com.hehe.doctor_service.controller;

import com.hehe.doctor_service.dto.request.CreationDoctorRequest;
import com.hehe.doctor_service.dto.request.RatingBumpRequest;
import com.hehe.doctor_service.dto.request.UpdateDoctorRequest;
import com.hehe.doctor_service.dto.response.ApiResponse;
import com.hehe.doctor_service.dto.response.DoctorResponse;
import com.hehe.doctor_service.dto.response.PageResponse;
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
    // Phan trang + loc chuyen khoa + tim ten. Doc tu cache (doctorService.getAll()).
    @GetMapping()
    public ApiResponse<PageResponse<DoctorResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String specialization,
            @RequestParam(required = false) String q) {
        String kw = q == null ? null : q.trim().toLowerCase();
        List<DoctorResponse> filtered = doctorService.getAll().stream()
                .filter(d -> specialization == null || specialization.isBlank()
                        || specialization.equals(d.getSpecialization()))
                .filter(d -> kw == null || kw.isBlank()
                        || (d.getFullName() != null && d.getFullName().toLowerCase().contains(kw)))
                .toList();
        return ApiResponse.<PageResponse<DoctorResponse>>builder()
                .data(PageResponse.of(filtered, page, size))
                .code(200)
                .message("Lấy danh sách bác sĩ thành công")
                .build();
    }

    // Batch: lay nhieu bac si theo danh sach id trong 1 request (thay cho N+1 goi tung cai).
    @GetMapping("/batch")
    public ApiResponse<List<DoctorResponse>> getByIds(@RequestParam List<String> ids) {
        return ApiResponse.<List<DoctorResponse>>builder()
                .data(doctorService.getByIds(ids))
                .code(200)
                .message("Lấy bác sĩ theo id thành công")
                .build();
    }

    // Danh sach chuyen khoa (cho dropdown loc o FE), lay tu cache.
    @GetMapping("/specializations")
    public ApiResponse<List<String>> specializations() {
        List<String> specs = doctorService.getAll().stream()
                .map(DoctorResponse::getSpecialization)
                .filter(s -> s != null && !s.isBlank())
                .distinct().sorted().toList();
        return ApiResponse.<List<String>>builder()
                .data(specs).code(200).message("Lấy chuyên khoa thành công").build();
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

    // Noi bo: cong 1 luot danh gia (appointment-service goi bang service-account, gated ADMIN).
    @PostMapping("/{id}/ratings")
    public ApiResponse<Void> addRating(@PathVariable String id,
                                       @Valid @RequestBody RatingBumpRequest request){
        doctorService.addRating(id, request.getStars());
        return ApiResponse.<Void>builder()
                .code(200)
                .message("Đã ghi nhận đánh giá")
                .build();
    }
}
