package com.hehe.appointment_service.service;

import com.hehe.appointment_service.client.DoctorClient;
import com.hehe.appointment_service.client.NotificationClient;
import com.hehe.appointment_service.client.PatientClient;
import com.hehe.appointment_service.dto.event.AppointmentNotificationEvent;
import com.hehe.appointment_service.dto.event.NotificationType;
import com.hehe.appointment_service.dto.request.CreationAppointmentRequest;
import com.hehe.appointment_service.dto.request.RateRequest;
import com.hehe.appointment_service.dto.request.UpdateAppointmentRequest;
import com.hehe.appointment_service.dto.response.AppointmentResponse;
import com.hehe.appointment_service.dto.response.DoctorDto;
import com.hehe.appointment_service.dto.response.PatientDto;
import com.hehe.appointment_service.dto.response.RatingResponse;
import com.hehe.appointment_service.entity.Appointment;
import com.hehe.appointment_service.entity.Rating;
import com.hehe.appointment_service.repository.RatingRepository;
import com.hehe.appointment_service.exception.AppException;
import com.hehe.appointment_service.exception.ErrorCode;
import com.hehe.appointment_service.mapper.AppointmentMapper;
import com.hehe.appointment_service.repository.AppointmentRepository;
import com.hehe.appointment_service.utils.AppointmentStatus;
import com.hehe.appointment_service.utils.SecurityUtils;
import com.hehe.appointment_service.dto.response.PageResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.experimental.NonFinal;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AppointmentService {

    static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    AppointmentRepository appointmentRepository;
    AppointmentMapper appointmentMapper;
    DoctorClient doctorClient;
    PatientClient patientClient;
    NotificationClient notificationClient;
    RatingRepository ratingRepository;
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

        // Bac si nghi ngay do?
        if (isDoctorOnLeave(request.getDoctorId(), appointmentTime)) {
            throw new AppException(ErrorCode.DOCTOR_ON_LEAVE);
        }

        // BR-01: chong trung lich
        if (isConflictCalendar(request.getDoctorId(), appointmentTime)) {
            throw new AppException(ErrorCode.APPOINTMENT_CONFLICT);
        }
        Appointment appointment = appointmentMapper.toEntity(request);

        // patientId lay tu token (goi patient-service /me), khong nhan tu client
        PatientDto patientDto= patientClient.getPatient();
        appointment.setPatientId(patientDto.getId());
        appointment.setDurationMinutes(durationMinutes);

        // Luoi chan CUOI cho race condition: unique index uq_doctor_slot (doctor_id, appointment_time)
        // WHERE status<>'CANCELLED'. saveAndFlush de vi pham no NGAY, bat truoc khi gui mail.
        Appointment saved;
        try {
            saved = appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException e) {
            // 2 nguoi dat cung slot cung luc -> DB tu choi cai sau
            throw new AppException(ErrorCode.APPOINTMENT_CONFLICT);
        }
        notify(NotificationType.APPOINTMENT_CREATED, saved);   // US-08 + len lich nhac US-09
        return appointmentMapper.toResponse(saved);
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

        // Bac si nghi ngay do?
        if (isDoctorOnLeave(request.getDoctorId(), appointmentTime)) {
            throw new AppException(ErrorCode.DOCTOR_ON_LEAVE);
        }

        // BR-01: chong trung lich
        if (appointmentRepository.isConflictOnUpdate(request.getDoctorId(),appointmentTime,durationMinutes,id)) {
            throw new AppException(ErrorCode.APPOINTMENT_CONFLICT);
        }

        appointment = appointmentMapper.updateEntity(appointment,request);
        // Cung luoi chan unique index khi doi gio hen (race condition luc dOi lich)
        Appointment saved;
        try {
            saved = appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.APPOINTMENT_CONFLICT);
        }
        notify(NotificationType.APPOINTMENT_RESCHEDULED, saved);
        return appointmentMapper.toResponse(saved);
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
        notify(NotificationType.APPOINTMENT_CANCELLED, appointment);   // + huy lich nhac
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
            notify(NotificationType.APPOINTMENT_CONFIRMED, appointment);
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
            notify(NotificationType.APPOINTMENT_COMPLETED, appointment);
            return true;
        }
        throw new AppException(ErrorCode.FORBIDDEN);
    }
    // GET /api/appointments/patients/me
    public List<AppointmentResponse> getMyPatientAppointments() {
        PatientDto me = patientClient.getPatient();
        List<Appointment> appts = appointmentRepository.findByPatientId(me.getId());
        List<AppointmentResponse> list = appts.stream().map(appointmentMapper::toResponse).toList();
        // Danh dau lich nao da danh gia -> FE an nut "Danh gia"
        java.util.Set<String> ratedIds = ratingRepository
                .findByAppointmentIdIn(appts.stream().map(Appointment::getId).toList())
                .stream().map(Rating::getAppointmentId).collect(java.util.stream.Collectors.toSet());
        list.forEach(r -> r.setRated(ratedIds.contains(r.getId())));
        return list;
    }

    // US: benh nhan danh gia bac si sau khi lich COMPLETED
    public void rate(String appointmentId, RateRequest request) {
        Appointment appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        PatientDto me = patientClient.getPatient();
        if (!appt.getPatientId().equals(me.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN);
        }
        if (appt.getStatus() != AppointmentStatus.COMPLETED) {
            throw new AppException(ErrorCode.RATING_NOT_ALLOWED);
        }
        if (ratingRepository.existsByAppointmentId(appointmentId)) {
            throw new AppException(ErrorCode.ALREADY_RATED);
        }

        Rating rating = new Rating();
        rating.setAppointmentId(appointmentId);
        rating.setDoctorId(appt.getDoctorId());
        rating.setPatientId(me.getId());
        rating.setStars(request.getStars());
        rating.setComment(request.getComment());
        ratingRepository.save(rating);

        // Cong aggregate ben doctor-service (best-effort: rating la nguon su that, aggregate
        // co the tinh lai neu lech). Loi bump khong lam hong viec danh gia.
        try {
            doctorClient.addRating(appt.getDoctorId(), request.getStars());
        } catch (Exception e) {
            log.warn("Khong cong duoc diem cho bac si {}: {}", appt.getDoctorId(), e.getMessage());
        }
    }

    // GET /api/appointments/doctors/{doctorId}/ratings - danh sach nhan xet cua 1 bac si
    public List<RatingResponse> getDoctorRatings(String doctorId) {
        List<Rating> ratings = ratingRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId);
        if (ratings.isEmpty()) return List.of();
        // Lam giau ten benh nhan (batch, service-account); loi lam giau -> de ten null
        java.util.Map<String, PatientDto> byId = java.util.Map.of();
        try {
            List<String> ids = ratings.stream().map(Rating::getPatientId).distinct().toList();
            byId = patientClient.getPatients(ids).stream()
                    .collect(java.util.stream.Collectors.toMap(PatientDto::getId, p -> p, (a, b) -> a));
        } catch (Exception e) {
            log.warn("Khong lam giau duoc ten benh nhan cho danh gia: {}", e.getMessage());
        }
        final java.util.Map<String, PatientDto> names = byId;
        return ratings.stream().map(r -> RatingResponse.builder()
                .stars(r.getStars())
                .comment(r.getComment())
                .patientName(names.containsKey(r.getPatientId()) ? names.get(r.getPatientId()).getFullName() : "Bệnh nhân")
                .createdAt(r.getCreatedAt())
                .build()).toList();
    }

    // GET /api/appointments/doctors/me
    public List<AppointmentResponse> getMyDoctorAppointments(LocalDate date) {
        DoctorDto me = doctorClient.getMe();
        Instant start = date.atStartOfDay(CLINIC_ZONE).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant();
        List<AppointmentResponse> list = appointmentRepository
                .findByDoctorIdAndAppointmentTimeBetween(me.getId(), start, end).stream()
                .map(appointmentMapper::toResponse).toList();
        return withPatientNames(list);   // lam giau ten benh nhan (bac si chi thay BN cua chinh minh)
    }
    // GET /api/appointments/doctors/{doctorId}/booked?date=...
    // Tra ve cac gio ĐA co lich (chua huy) cua bac si trong 1 ngay, de FE lam mo slot da dat.
    // Chi tra thoi gian (khong lo thong tin benh nhan) nen benh nhan khac xem duoc.
    public List<Instant> getBookedTimes(String doctorId, LocalDate date) {
        Instant start = date.atStartOfDay(CLINIC_ZONE).toInstant();
        Instant end = date.plusDays(1).atStartOfDay(CLINIC_ZONE).toInstant();
        return appointmentRepository.findByDoctorIdAndAppointmentTimeBetween(doctorId, start, end).stream()
                .filter(a -> a.getStatus() != AppointmentStatus.CANCELLED)
                .map(Appointment::getAppointmentTime)
                .toList();
    }

    private boolean isConflictCalendar(String doctorId, Instant appointmentTime) {
        return appointmentRepository.isConflict(doctorId, appointmentTime, durationMinutes);
    }

    // Ngay hen (theo gio phong kham) co nam trong ngay nghi cua bac si khong
    private boolean isDoctorOnLeave(String doctorId, Instant appointmentTime) {
        LocalDate apptDate = appointmentTime.atZone(CLINIC_ZONE).toLocalDate();
        return doctorClient.getLeaveDates(doctorId).contains(apptDate);
    }
    public AppointmentResponse getOne(String id){
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.APPOINTMENT_NOT_FOUND));

        // BR-06: ADMIN xem tat ca; DOCTOR xem lich cua minh; PATIENT xem lich cua minh
        if (SecurityUtils.hasRole("ADMIN")) {
            // xem tat ca
        } else if (SecurityUtils.hasRole("DOCTOR")) {
            if (!appointment.getDoctorId().equals(doctorClient.getMe().getId())) {
                throw new AppException(ErrorCode.FORBIDDEN);
            }
        } else {
            if (!appointment.getPatientId().equals(patientClient.getPatient().getId())) {
                throw new AppException(ErrorCode.FORBIDDEN);
            }
        }
        return appointmentMapper.toResponse(appointment);
    }
    // ADMIN xem toan bo lich hen - PHAN TRANG DB (khong tai toan bang), loc theo trang thai.
    public PageResponse<AppointmentResponse> getAll(int page, int size, AppointmentStatus status) {
        int safeSize = Math.min(Math.max(size, 1), 100);   // page size toi da 100
        Pageable pageable = PageRequest.of(Math.max(page, 0), safeSize,
                Sort.by(Sort.Direction.DESC, "appointmentTime"));
        Page<Appointment> pg = (status == null)
                ? appointmentRepository.findAll(pageable)
                : appointmentRepository.findByStatus(status, pageable);
        // Chi lam giau ten cho content cua TRANG hien tai (<= 100 ban ghi) -> khong N+1, id it
        List<AppointmentResponse> content = withPatientNames(
                pg.getContent().stream().map(appointmentMapper::toResponse).toList());
        return new PageResponse<>(content, pg.getNumber(), pg.getSize(),
                pg.getTotalPages(), pg.getTotalElements());
    }

    // Lam giau ten benh nhan qua batch (token service-account). Loi lam giau KHONG lam hong
    // danh sach lich -> chi de patientName null. Day cung la cho enforce BR-06: bac si/admin
    // khong con goi thang /patients/batch nua.
    private List<AppointmentResponse> withPatientNames(List<AppointmentResponse> list) {
        if (list.isEmpty()) return list;
        try {
            List<String> ids = list.stream()
                    .map(AppointmentResponse::getPatientId)
                    .filter(java.util.Objects::nonNull).distinct().toList();
            java.util.Map<String, PatientDto> byId = patientClient.getPatients(ids).stream()
                    .collect(java.util.stream.Collectors.toMap(PatientDto::getId, p -> p, (a, b) -> a));
            list.forEach(r -> {
                PatientDto p = byId.get(r.getPatientId());
                if (p != null) {
                    r.setPatientName(p.getFullName());
                    r.setPatientPhone(p.getPhone());
                }
            });
        } catch (Exception e) {
            log.warn("Khong lam giau duoc ten benh nhan: {}", e.getMessage());
        }
        return list;
    }
    private void notify(NotificationType type, Appointment appt) {
        PatientDto p = patientClient.getPatient(appt.getPatientId());
        DoctorDto  d = doctorClient.getDoctor(appt.getDoctorId());

        notificationClient.send(AppointmentNotificationEvent.builder()
                .type(type)
                .appointmentId(appt.getId())
                .patientName(p.getFullName()).patientEmail(p.getEmail())
                .doctorName(d.getFullName()).specialization(d.getSpecialization())
                .appointmentTime(appt.getAppointmentTime())
                .reason(appt.getReason())
                .build());
    }

}
