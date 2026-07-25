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
import com.hehe.appointment_service.utils.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

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

        return appointmentMapper.toResponse(appointmentRepository.save(appointment));
    }

    public boolean cancel(String id){
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(()->new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        PatientDto me = patientClient.getPatient();
        if (!appointment.getPatientId().equals(me.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        if (appointment.getStatus() == AppointmentStatus.COMPLETED) {
            throw new AppException(ErrorCode.CANNOT_MODIFY_COMPLETED);
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointmentRepository.save(appointment);
        return true;
    }
    public boolean confirm(String id){
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(()->new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        DoctorDto me = doctorClient.getMe();
        if (appointment.getDoctorId().equals(me.getId())
        || SecurityUtils.hasRole("ADMIN")) {
            if (appointment.getStatus() == AppointmentStatus.COMPLETED
                    || appointment.getStatus() == AppointmentStatus.CANCELLED) {
                throw new AppException(ErrorCode.CANNOT_MODIFY_COMPLETED);
            }

            appointment.setStatus(AppointmentStatus.CONFIRMED);
            appointmentRepository.save(appointment);
            return true;
        }
        throw new AppException(ErrorCode.FORBIDDEN);
    }
    public boolean complete(String id){
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(()->new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        DoctorDto me = doctorClient.getMe();
        if (appointment.getDoctorId().equals(me.getId())
               ) {
            if (appointment.getStatus() != AppointmentStatus.CONFIRMED
                   ) {
                throw new AppException(ErrorCode.CANNOT_MODIFY_COMPLETED);
            }

            appointment.setStatus(AppointmentStatus.COMPLETED);
            appointmentRepository.save(appointment);
            return true;
        }
        throw new AppException(ErrorCode.FORBIDDEN);
    }
    // GET /api/appointments/patients/me
    public List<AppointmentResponse> getMyPatientAppointments() {
        PatientDto me = patientClient.getPatient();
        return appointmentRepository.findByPatientId(me.getId()).stream()
                .map(appointmentMapper::toResponse).toList();
    }

    // GET /api/appointments/doctors/me
    public List<AppointmentResponse> getMyDoctorAppointments(LocalDate date) {
        DoctorDto me = doctorClient.getMe();
        return appointmentRepository.findByDoctorIdAndDate(me.getId(), date).stream()
                .map(appointmentMapper::toResponse).toList();
    }
    private boolean isConflictCalendar(String doctorId, Instant appointmentTime) {
        return appointmentRepository.isConflict(doctorId, appointmentTime, durationMinutes);
    }
    public AppointmentResponse getOne(String id){
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        // BR-06: chi chu lich hoac ADMIN
        if (!SecurityUtils.hasRole("ADMIN")) {
            PatientDto me = patientClient.getPatient();
            if (!appointment.getPatientId().equals(me.getId())) {
                throw new AppException(ErrorCode.FORBIDDEN);
            }
        }
        return appointmentMapper.toResponse(appointment);
    }
    public List<AppointmentResponse> getAll(){
        return appointmentRepository.findAll().stream()
                .map(appointmentMapper::toResponse).toList();
    }

}
