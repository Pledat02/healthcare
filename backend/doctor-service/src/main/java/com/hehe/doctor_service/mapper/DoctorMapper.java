package com.hehe.doctor_service.mapper;

import com.hehe.doctor_service.dto.request.CreationDoctorRequest;
import com.hehe.doctor_service.dto.request.UpdateDoctorRequest;
import com.hehe.doctor_service.dto.response.DoctorResponse;
import com.hehe.doctor_service.entity.Doctor;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface DoctorMapper  {

     @Mapping(target = "avgRating", ignore = true)
     @Mapping(target = "ratingCount", ignore = true)
     DoctorResponse toResponse(Doctor doctor);

     // Tinh trung binh sao tu (ratingSum, ratingCount); null-guard cho row cu.
     @AfterMapping
     default void computeRating(Doctor doctor, @MappingTarget DoctorResponse res) {
          int count = doctor.getRatingCount() == null ? 0 : doctor.getRatingCount();
          int sum = doctor.getRatingSum() == null ? 0 : doctor.getRatingSum();
          res.setRatingCount(count);
          res.setAvgRating(count > 0 ? Math.round((double) sum / count * 10.0) / 10.0 : null);
     }

     Doctor toEntity(CreationDoctorRequest doctorRequest);

     @Mapping(target = "id",ignore = true)
     @Mapping(target = "keycloakId", ignore = true)
     @Mapping(target = "createdAt", ignore = true)
     @Mapping(target = "updatedTime", ignore = true)
     Doctor updateEntity (@MappingTarget Doctor doctor, UpdateDoctorRequest doctorRequest);
}
