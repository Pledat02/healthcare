package com.hehe.appointment_service.service;

import com.hehe.appointment_service.client.DoctorClient;
import com.hehe.appointment_service.client.PatientClient;
import com.hehe.appointment_service.dto.request.CreationAppointmentRequest;
import com.hehe.appointment_service.dto.request.UpdateAppointmentRequest;
import com.hehe.appointment_service.dto.response.AppointmentResponse;
import com.hehe.appointment_service.dto.response.DoctorDto;
import com.hehe.appointment_service.dto.response.PatientDto;
import com.hehe.appointment_service.entity.Appointment;
import com.hehe.appointment_service.exception.AppException;
import com.hehe.appointment_service.exception.ErrorCode;
import com.hehe.appointment_service.mapper.AppointmentMapper;
import com.hehe.appointment_service.repository.AppointmentRepository;
import com.hehe.appointment_service.utils.AppointmentStatus;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AppointmentService {

    static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    AppointmentRepository appointmentRepository;
    AppointmentMapper appointmentMapper;
    DoctorClient doctorClient;
    PatientClient patientClient;

    @Value("${appointment.duration-minutes}")
    @NonFinal
    int durationMinutes;

    public AppointmentResponse create(CreationAppointmentRequest request) {

        Instant appointmentTime = request.getAppointmentTime();

        // BR-02: khong dat lich trong qua khu
        if (appointmentTime.isBefore(Instant.now())) {
            throw new AppException(ErrorCode.APPOINTMENT_TIME_IN_PAST);
        }

        // US-05 + BR-03: bac si ton tai? gio hen co trong gio lam viec?
        DoctorDto doctor = doctorClient.getDoctor(request.getDoctorId());
        LocalTime start = appointmentTime.atZone(CLINIC_ZONE).toLocalTime();
        LocalTime end = start.plusMinutes(durationMinutes);

        if (start.isBefore(doctor.getWorkStartTime()) || end.isAfter(doctor.getWorkEndTime())) {
            throw new AppException(ErrorCode.OUTSIDE_WORKING_HOURS);
        }

        // BR-01: chong trung lich
        if (isConflictCalendar(request.getDoctorId(), appointmentTime)) {
            throw new AppException(ErrorCode.APPOINTMENT_CONFLICT);
        }
        Appointment appointment = appointmentMapper.toEntity(request);

        // TODO: gan patientId lay tu token (goi patient-service /me)
        PatientDto patientDto= patientClient.getPatient();
        appointment.setPatientId(patientDto.getId());
        appointment.setDurationMinutes(durationMinutes);

        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    public AppointmentResponse update(String id,UpdateAppointmentRequest request) {

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(()->new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        PatientDto me = patientClient.getPatient();
        if (!appointment.getPatientId().equals(me.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new AppException(ErrorCode.CANNOT_MODIFY_COMPLETED);
        }
        Instant appointmentTime = request.getAppointmentTime();

        // BR-02: khong dat lich trong qua khu
        if (appointmentTime.isBefore(Instant.now())) {
            throw new AppException(ErrorCode.APPOINTMENT_TIME_IN_PAST);
        }

        // US-05 + BR-03: bac si ton tai? gio hen co trong gio lam viec?
        DoctorDto doctor = doctorClient.getDoctor(request.getDoctorId());
        LocalTime start = appointmentTime.atZone(CLINIC_ZONE).toLocalTime();
        LocalTime end = start.plusMinutes(durationMinutes);

        if (start.isBefore(doctor.getWorkStartTime()) || end.isAfter(doctor.getWorkEndTime())) {
            throw new AppException(ErrorCode.OUTSIDE_WORKING_HOURS);
        }

        // BR-01: chong trung lich
        if (appointmentRepository.isConflictOnUpdate(request.getDoctorId(),appointmentTime,durationMinutes,id)) {
            throw new AppException(ErrorCode.APPOINTMENT_CONFLICT);
        }
         appointment = appointmentMapper.updateEntity(appointment,request);

        appointment.setDurationMinutes(durationMinutes);

        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }


    private boolean isConflictCalendar(String doctorId, Instant appointmentTime) {
        return appointmentRepository.isConflict(doctorId, appointmentTime, durationMinutes);
    }

}
