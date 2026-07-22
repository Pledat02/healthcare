package com.hehe.doctor_service.mapper;

import com.hehe.doctor_service.dto.request.CreationDoctorRequest;
import com.hehe.doctor_service.dto.request.UpdateDoctorRequest;
import com.hehe.doctor_service.dto.response.DoctorResponse;
import com.hehe.doctor_service.entity.Doctor;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import javax.swing.text.html.parser.Entity;

@Mapper(componentModel = "Spring")
public interface DoctorMapper  {

     DoctorResponse toResponse(Doctor doctor);

     Doctor toEntity(CreationDoctorRequest doctorRequest);

     @Mapping(target = "id",ignore = true)
     @Mapping(target = "keyCloakId", ignore = true)
     @Mapping(target = "updatedTime",ignore = true)
     @Mapping(target = "createdTime",ignore = true)
     void updateEntity (Doctor doctor, UpdateDoctorRequest doctorRequest);
}
