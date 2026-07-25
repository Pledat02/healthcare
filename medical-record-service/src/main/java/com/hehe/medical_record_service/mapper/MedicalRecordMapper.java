package com.hehe.medical_record_service.mapper;

import com.hehe.medical_record_service.dto.request.CreationMedicalRecordRequest;
import com.hehe.medical_record_service.dto.response.MedicalRecordResponse;
import com.hehe.medical_record_service.entity.MedicalRecord;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MedicalRecordMapper {
    MedicalRecord toEntity(CreationMedicalRecordRequest request);
    MedicalRecordResponse toResponse(MedicalRecord medicalRecord);
}
