package com.hehe.appointment_service.mapper;

import com.hehe.appointment_service.dto.request.CreationAppointmentRequest;
import com.hehe.appointment_service.dto.request.UpdateAppointmentRequest;
import com.hehe.appointment_service.dto.response.AppointmentResponse;
import com.hehe.appointment_service.entity.Appointment;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {
    Appointment  toEntity(CreationAppointmentRequest request);
    AppointmentResponse toResponse(Appointment appointment);

    Appointment updateEntity(Appointment appointment, UpdateAppointmentRequest request);
}
