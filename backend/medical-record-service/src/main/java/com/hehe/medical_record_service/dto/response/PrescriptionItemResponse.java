package com.hehe.medical_record_service.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionItemResponse {
    String id;

    String medicineName;

    String dosage;

    int quantity;

    String instruction;
}
