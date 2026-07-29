package com.hehe.medical_record_service.dto.request;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionItemRequest {
    String medicineName;

    String dosage;

    int quantity;

    String instruction;
}
