package com.hehe.appointment_service.mapper;

import com.hehe.appointment_service.dto.request.CreationAppointmentRequest;
import com.hehe.appointment_service.dto.request.UpdateAppointmentRequest;
import com.hehe.appointment_service.dto.response.AppointmentResponse;
import com.hehe.appointment_service.entity.Appointment;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {
    Appointment toEntity(CreationAppointmentRequest request);

    AppointmentResponse toResponse(Appointment appointment);

    Appointment updateEntity(@MappingTarget Appointment appointment, UpdateAppointmentRequest request);
}
