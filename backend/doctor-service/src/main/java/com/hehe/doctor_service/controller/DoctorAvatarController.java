package com.hehe.doctor_service.controller;

import com.hehe.doctor_service.dto.response.ApiResponse;
import com.hehe.doctor_service.dto.response.DoctorAvatarResponse;
import com.hehe.doctor_service.service.DoctorAvatarService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
public class DoctorAvatarController {
    private final DoctorAvatarService avatarService;

    @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DoctorAvatarResponse> uploadMine(@RequestPart("file") MultipartFile file) {
        return ok(avatarService.uploadMine(file), "Anh da duoc gui va dang cho duyet");
    }

    @PutMapping(value = "/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<DoctorAvatarResponse> uploadApproved(@PathVariable String id,
                                                            @RequestPart("file") MultipartFile file) {
        return ok(avatarService.uploadApproved(id, file), "Da cap nhat anh bac si");
    }

    @GetMapping("/{id}/avatar/review")
    public ApiResponse<DoctorAvatarResponse> review(@PathVariable String id) {
        return ok(avatarService.getReview(id), "Lay anh cho duyet thanh cong");
    }

    @PatchMapping("/{id}/avatar/approve")
    public ApiResponse<DoctorAvatarResponse> approve(@PathVariable String id) {
        return ok(avatarService.approve(id), "Da duyet anh bac si");
    }

    @PatchMapping("/{id}/avatar/reject")
    public ApiResponse<DoctorAvatarResponse> reject(@PathVariable String id) {
        return ok(avatarService.reject(id), "Da tu choi anh bac si");
    }

    private ApiResponse<DoctorAvatarResponse> ok(DoctorAvatarResponse data, String message) {
        return ApiResponse.<DoctorAvatarResponse>builder().code(200).message(message).data(data).build();
    }
}
