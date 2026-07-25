package com.hehe.medical_record_service.dto.response;

import com.hehe.medical_record_service.entity.PrescriptionItem;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalRecordResponse {
    String id;

    String appointmentId;

    DoctorDto doctor;

     PatientDto patient;

   String diagnosis;

   String notes;

   Set<PrescriptionItem> prescriptionItems;
}
