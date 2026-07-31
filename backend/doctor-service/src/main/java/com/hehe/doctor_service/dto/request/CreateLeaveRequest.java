package com.hehe.doctor_service.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateLeaveRequest {

    @NotNull(message = "Ngày nghỉ không được để trống")
    LocalDate leaveDate;

    String reason;
}
