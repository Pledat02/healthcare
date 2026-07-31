package com.hehe.appointment_service.dto.response;

import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

// Ngay nghi cua bac si (lay tu doctor-service) - dung de tu choi dat/doi lich trung ngay nghi
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DoctorLeaveDto {
    String id;
    LocalDate leaveDate;
    String reason;
}
