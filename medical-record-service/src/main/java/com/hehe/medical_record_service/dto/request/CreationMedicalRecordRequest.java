package com.hehe.medical_record_service.dto.request;

import com.hehe.medical_record_service.entity.PrescriptionItem;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.Set;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreationMedicalRecordRequest {

    String appointmentId;

    String patientId;

     String doctorId;

   String diagnosis;

   String notes;

   Set<PrescriptionItem> prescriptionItems;
}
