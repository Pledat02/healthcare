package com.hehe.medical_record_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalRecordResponse {
    String id;

    String appointmentId;

    // Soft ref sang service khac -> tra ve id, khong long ca object
    String doctorId;

    String patientId;

    String diagnosis;

    String notes;

    Instant createdAt;

    // Phai la DTO, KHONG dung entity PrescriptionItem: entity co back-reference
    // 'medicalRecord' -> Jackson lap vo tan khi serialize.
    List<PrescriptionItemResponse> prescriptionItems;
}
