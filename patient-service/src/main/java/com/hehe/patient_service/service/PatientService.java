package com.hehe.patient_service.service;

import com.hehe.patient_service.dto.request.CreationPatientRequest;
import com.hehe.patient_service.dto.request.UpdationPatientRequest;
import com.hehe.patient_service.dto.response.PatientResponse;
import com.hehe.patient_service.entity.Patient;
import com.hehe.patient_service.exception.AppException;
import com.hehe.patient_service.exception.ErrorCode;
import com.hehe.patient_service.helper.SecurityUtil;
import com.hehe.patient_service.mapper.PatientMapper;
import com.hehe.patient_service.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PatientService {
    private final PatientMapper patientMapper;
    private final PatientRepository patientRepository;

    public PatientResponse create(CreationPatientRequest request , String keycloakId) {
        // case US-02
        if (patientRepository.existsByKeycloakId(keycloakId)) {
            throw new AppException(ErrorCode.PATIENT_ALREADY_EXISTS);
        }
        Patient patient = patientMapper.toEntity(request);
        patient.setKeycloakId(keycloakId);
        return patientMapper.toResponse(patientRepository.save(patient));
    }

    public PatientResponse update(String id, UpdationPatientRequest request) {
        Patient patient = patientRepository.findById(id).orElseThrow(() ->
                new AppException(ErrorCode.PATIENT_NOT_FOUND));
        checkAccessRole(patient);
        patientMapper.updateEntity(patient,request);
        Patient updatedPatient = patientRepository.save(patient);
        return patientMapper.toResponse(updatedPatient);
    }

    public PatientResponse getOne(String id) {
        Patient patient = patientRepository.findById(id).orElseThrow(() ->
                new AppException(ErrorCode.PATIENT_NOT_FOUND));
        checkAccessRole(patient);
        return patientMapper.toResponse(patient);
    }

    public PatientResponse getMe(){
        String idKeyCloak = SecurityUtil.getCurrentKeyCloakId();
        return patientMapper.toResponse(patientRepository.findByKeycloakId(idKeyCloak).orElseThrow(() ->
                new AppException(ErrorCode.PATIENT_NOT_FOUND)));
    }

    public List<PatientResponse> getAll() {
        if (!SecurityUtil.isAdmin()) throw new AppException(ErrorCode.FORBIDDEN);
        return patientRepository.findAll().stream()
                .map(patientMapper::toResponse).toList();

    }

    public void delete(String id) {
        patientRepository.deleteById(id);

    }

    public void checkAccessRole(Patient patient){
        if(!SecurityUtil.isAdmin() && !patient.getKeycloakId().equals(SecurityUtil.getCurrentKeyCloakId()))
            throw new AppException(ErrorCode.FORBIDDEN);
    }

}
