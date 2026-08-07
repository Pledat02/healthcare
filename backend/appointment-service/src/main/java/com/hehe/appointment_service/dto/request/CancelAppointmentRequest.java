package com.hehe.appointment_service.dto.request;

import lombok.Data;

// Ly do huy lich. Admin huy thi nen ghi ly do (gui kem email cho benh nhan);
// benh nhan tu huy co the bo trong.
@Data
public class CancelAppointmentRequest {
    String reason;
}
