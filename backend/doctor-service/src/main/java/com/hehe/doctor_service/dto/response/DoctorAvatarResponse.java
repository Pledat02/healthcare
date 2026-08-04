package com.hehe.doctor_service.dto.response;

import com.hehe.doctor_service.entity.DoctorAvatarStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DoctorAvatarResponse {
    String doctorId;
    String avatarUrl;
    String pendingAvatarUrl;
    DoctorAvatarStatus status;
}
