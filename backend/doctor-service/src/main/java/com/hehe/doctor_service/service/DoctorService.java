package com.hehe.doctor_service.service;

import com.hehe.doctor_service.client.KeycloakAdminClient;
import com.hehe.doctor_service.dto.request.CreationDoctorRequest;
import com.hehe.doctor_service.dto.request.UpdateDoctorRequest;
import com.hehe.doctor_service.dto.response.DoctorResponse;
import com.hehe.doctor_service.entity.Doctor;
import com.hehe.doctor_service.exception.AppException;
import com.hehe.doctor_service.exception.ErrorCode;
import com.hehe.doctor_service.mapper.DoctorMapper;
import com.hehe.doctor_service.repository.DoctorRepository;
import com.hehe.doctor_service.utils.SecurityUtils;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE,makeFinal = true)
@RequiredArgsConstructor
public class DoctorService {

    // Thong nhat voi patient-service: batch toi da 100 ID
    private static final int MAX_BATCH_IDS = 100;

    DoctorMapper doctorMapper ;
    DoctorRepository doctorRepository;
    KeycloakAdminClient keycloakAdminClient;
    SupabaseAvatarStorage avatarStorage;

    // US-03: admin them bac si -> tao luon tai khoan Keycloak (role DOCTOR)
    // va noi voi ho so qua keycloakId, de bac si dang nhap duoc ngay.
    // Them bac si moi -> danh sach cu trong cache khong con dung -> xoa cache "doctors".
    @CacheEvict(value = "doctors", allEntries = true)
    public DoctorResponse create(CreationDoctorRequest request){
        String keycloakId = keycloakAdminClient.createDoctorUser(
                request.getUsername(), request.getPassword(),
                request.getFullName(), request.getEmail());
        try {
            Doctor doctor = doctorMapper.toEntity(request);
            doctor.setKeycloakId(keycloakId);
            return toResponse(doctorRepository.save(doctor));
        } catch (RuntimeException e) {
            // Luu DB that bai -> go bo tai khoan vua tao, tranh user mo coi
            keycloakAdminClient.deleteUser(keycloakId);
            throw e;
        }
    }
    @Cacheable(value = "doctor", key = "#id")
    public DoctorResponse getOne(String id){

        Doctor doctor = doctorRepository.findById(id).orElseThrow(
                ()-> new AppException(ErrorCode.DOCTOR_NOT_FOUND)
        );
        // Ho so bac si cho MOI user da dang nhap xem (PRD: benh nhan can xem bac si de dat lich)
        // -> khong kiem chu so huu o day.
        return toResponse(doctor);
    }

    @Cacheable(value = "doctors", key = "'all'")
    public List<DoctorResponse> getAll(){
        // check admin

        return doctorRepository.findAll().stream().
        map(this::toResponse).toList();
    }

    // Batch: lay nhieu bac si theo id. THONG NHAT voi patient-service - cung dung findAllById
    // (truy van IN theo khoa chinh, co index). Cache van phuc vu endpoint danh sach/phan trang (getAll).
    public List<DoctorResponse> getByIds(List<String> ids) {
        if (ids == null || ids.size() > MAX_BATCH_IDS)
            throw new AppException(ErrorCode.TOO_MANY_IDS);
        return doctorRepository.findAllById(ids).stream()
                .map(this::toResponse).toList();
    }

    // Sua bac si -> xoa ca cache chi tiet (dung id) va danh sach.
    @Caching(evict = {
            @CacheEvict(value = "doctor", key = "#id"),
            @CacheEvict(value = "doctors", allEntries = true)
    })
    public DoctorResponse update(String id, UpdateDoctorRequest request){
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(
                        () -> new AppException(ErrorCode.DOCTOR_NOT_FOUND)
                );
        // Hien tai SUA ho so bac si chi cho ADMIN (SecurityConfig chan PUT /api/doctors/** = ADMIN).
        // Neu sau nay mo cho "bac si chinh chu tu sua" (PRD): them PUT cho role DOCTOR o SecurityConfig
        // va bat lai kiem chu so huu: if (!SecurityUtils.isAccessed(doctor)) throw FORBIDDEN;
         doctor = doctorMapper.updateEntity(doctor,request);
        doctorRepository.save(doctor);
        return toResponse(doctor);
    }
    public DoctorResponse getMe(){
        String idKeyCloak = SecurityUtils.getKeyCloakId();
        return toResponse(doctorRepository.findByKeycloakId(idKeyCloak).orElseThrow(() ->
                new AppException(ErrorCode.DOCTOR_NOT_FOUND)));
    }

    // Cong them 1 luot danh gia (goi tu appointment-service bang token service-account).
    // Evict cache de danh sach/chi tiet bac si hien diem moi.
    @Caching(evict = {
            @CacheEvict(value = "doctor", key = "#doctorId"),
            @CacheEvict(value = "doctors", allEntries = true)
    })
    public void addRating(String doctorId, int stars) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_NOT_FOUND));
        int sum = doctor.getRatingSum() == null ? 0 : doctor.getRatingSum();
        int count = doctor.getRatingCount() == null ? 0 : doctor.getRatingCount();
        doctor.setRatingSum(sum + stars);
        doctor.setRatingCount(count + 1);
        doctorRepository.save(doctor);
    }

    @Caching(evict = {
            @CacheEvict(value = "doctor", key = "#id"),
            @CacheEvict(value = "doctors", allEntries = true)
    })
    public void delete(String id){
        // Xoa ca tai khoan Keycloak de khong con user mo coi khong ai dung
        doctorRepository.findById(id)
                .ifPresent(d -> keycloakAdminClient.deleteUser(d.getKeycloakId()));
        doctorRepository.deleteById(id);
    }

    private DoctorResponse toResponse(Doctor doctor) {
        DoctorResponse response = doctorMapper.toResponse(doctor);
        response.setAvatarUrl(avatarStorage.publicUrl(doctor.getAvatarPath()));
        return response;
    }

}
