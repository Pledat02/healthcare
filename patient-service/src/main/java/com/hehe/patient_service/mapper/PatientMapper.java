package com.hehe.patient_service.mapper;


import com.hehe.patient_service.dto.request.CreationPatientRequest;
import com.hehe.patient_service.dto.request.UpdationPatientRequest;
import com.hehe.patient_service.dto.response.PatientResponse;
import com.hehe.patient_service.entity.Patient;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PatientMapper {
    Patient toEntity(CreationPatientRequest request);
    PatientResponse toResponse(Patient patient);
    // Trong PatientMapper
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "createdDate", ignore = true)
    @Mapping(target = "updatedDate", ignore = true)
    void updateEntity(@MappingTarget Patient patient, UpdationPatientRequest request);
}
