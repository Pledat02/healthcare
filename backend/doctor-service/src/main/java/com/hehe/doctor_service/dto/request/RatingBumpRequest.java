package com.hehe.doctor_service.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

// Cong 1 luot danh gia vao bac si (goi noi bo tu appointment-service)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RatingBumpRequest {
    @Min(1) @Max(5)
    int stars;
}
