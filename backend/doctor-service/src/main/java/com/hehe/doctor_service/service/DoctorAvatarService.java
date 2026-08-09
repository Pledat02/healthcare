package com.hehe.doctor_service.service;

import com.hehe.doctor_service.config.DoctorAvatarProperties;
import com.hehe.doctor_service.dto.response.DoctorAvatarResponse;
import com.hehe.doctor_service.entity.Doctor;
import com.hehe.doctor_service.entity.DoctorAvatarStatus;
import com.hehe.doctor_service.exception.AppException;
import com.hehe.doctor_service.exception.ErrorCode;
import com.hehe.doctor_service.repository.DoctorRepository;
import com.hehe.doctor_service.utils.SecurityUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Iterator;
import java.util.UUID;

@Service
public class DoctorAvatarService {
    private static final int OUTPUT_SIZE = 512;
    private static final long MAX_DECODED_PIXELS = 40_000_000L;

    private final DoctorRepository doctorRepository;
    private final SupabaseAvatarStorage storage;
    private final DoctorAvatarProperties properties;

    public DoctorAvatarService(DoctorRepository doctorRepository, SupabaseAvatarStorage storage,
                               DoctorAvatarProperties properties) {
        this.doctorRepository = doctorRepository;
        this.storage = storage;
        this.properties = properties;
        ImageIO.setUseCache(false);
    }

    @Transactional
    @CacheEvict(value = "doctors", allEntries = true)
    public DoctorAvatarResponse uploadMine(MultipartFile file) {
        Doctor doctor = doctorRepository.findByKeycloakId(SecurityUtils.getKeyCloakId())
                .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_NOT_FOUND));
        byte[] jpeg = normalize(file);
        String pendingPath = newObjectPath(doctor.getId());
        storage.uploadJpeg(pendingPath, jpeg);
        String previousPendingPath = doctor.getPendingAvatarPath();
        doctor.setPendingAvatarPath(pendingPath);
        doctor.setAvatarStatus(DoctorAvatarStatus.PENDING);
        doctor.setAvatarUpdatedAt(Instant.now());
        try {
            doctorRepository.saveAndFlush(doctor);
        } catch (RuntimeException ex) {
            storage.deleteQuietly(pendingPath);
            throw ex;
        }
        storage.deleteQuietly(previousPendingPath);
        return response(doctor);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "doctor", key = "#doctorId"),
            @CacheEvict(value = "doctors", allEntries = true)
    })
    public DoctorAvatarResponse uploadApproved(String doctorId, MultipartFile file) {
        Doctor doctor = find(doctorId);
        byte[] jpeg = normalize(file);
        return storeApproved(doctor, jpeg);
    }

    @Transactional
    public boolean seedApproved(String email, byte[] file) {
        Doctor doctor = doctorRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_NOT_FOUND));
        if (doctor.getAvatarPath() != null && !doctor.getAvatarPath().isBlank()) return false;
        storeApproved(doctor, normalize(file));
        return true;
    }

    private DoctorAvatarResponse storeApproved(Doctor doctor, byte[] jpeg) {
        String doctorId = doctor.getId();
        String newPath = newObjectPath(doctorId);
        storage.uploadJpeg(newPath, jpeg);
        String oldPath = doctor.getAvatarPath();
        String oldPendingPath = doctor.getPendingAvatarPath();
        doctor.setAvatarPath(newPath);
        doctor.setPendingAvatarPath(null);
        doctor.setAvatarStatus(DoctorAvatarStatus.APPROVED);
        doctor.setAvatarUpdatedAt(Instant.now());
        try {
            doctorRepository.saveAndFlush(doctor);
        } catch (RuntimeException ex) {
            storage.deleteQuietly(newPath);
            throw ex;
        }
        storage.deleteQuietly(oldPendingPath);
        storage.deleteQuietly(oldPath);
        return response(doctor);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "doctor", key = "#doctorId"),
            @CacheEvict(value = "doctors", allEntries = true)
    })
    public DoctorAvatarResponse approve(String doctorId) {
        Doctor doctor = find(doctorId);
        if (doctor.getPendingAvatarPath() == null) throw new AppException(ErrorCode.AVATAR_NOT_PENDING);
        String oldPath = doctor.getAvatarPath();
        doctor.setAvatarPath(doctor.getPendingAvatarPath());
        doctor.setPendingAvatarPath(null);
        doctor.setAvatarStatus(DoctorAvatarStatus.APPROVED);
        doctor.setAvatarUpdatedAt(Instant.now());
        doctorRepository.saveAndFlush(doctor);
        storage.deleteQuietly(oldPath);
        return response(doctor);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "doctor", key = "#doctorId"),
            @CacheEvict(value = "doctors", allEntries = true)
    })
    public DoctorAvatarResponse reject(String doctorId) {
        Doctor doctor = find(doctorId);
        if (doctor.getPendingAvatarPath() == null) throw new AppException(ErrorCode.AVATAR_NOT_PENDING);
        String rejectedPath = doctor.getPendingAvatarPath();
        doctor.setPendingAvatarPath(null);
        doctor.setAvatarStatus(DoctorAvatarStatus.REJECTED);
        doctor.setAvatarUpdatedAt(Instant.now());
        doctorRepository.saveAndFlush(doctor);
        storage.deleteQuietly(rejectedPath);
        return response(doctor);
    }

    public DoctorAvatarResponse getReview(String doctorId) {
        return response(find(doctorId));
    }

    private Doctor find(String doctorId) {
        return doctorRepository.findById(doctorId)
                .orElseThrow(() -> new AppException(ErrorCode.DOCTOR_NOT_FOUND));
    }

    private DoctorAvatarResponse response(Doctor doctor) {
        return DoctorAvatarResponse.builder()
                .doctorId(doctor.getId())
                .avatarUrl(storage.publicUrl(doctor.getAvatarPath()))
                .pendingAvatarUrl(storage.publicUrl(doctor.getPendingAvatarPath()))
                .status(doctor.getAvatarStatus() == null ? DoctorAvatarStatus.NONE : doctor.getAvatarStatus())
                .build();
    }

    private String newObjectPath(String doctorId) {
        return "doctors/" + doctorId + "/" + UUID.randomUUID() + ".jpg";
    }

    private byte[] normalize(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new AppException(ErrorCode.AVATAR_EMPTY);
        if (file.getSize() > properties.getMaxBytes()) throw new AppException(ErrorCode.AVATAR_TOO_LARGE);
        try {
            return normalize(file.getBytes());
        } catch (IOException ex) {
            throw new AppException(ErrorCode.AVATAR_INVALID, ex);
        }
    }

    private byte[] normalize(byte[] file) {
        if (file == null || file.length == 0) throw new AppException(ErrorCode.AVATAR_EMPTY);
        if (file.length > properties.getMaxBytes()) throw new AppException(ErrorCode.AVATAR_TOO_LARGE);
        if (!isJpeg(file) && !isPng(file)) throw new AppException(ErrorCode.AVATAR_INVALID);
        try {
            BufferedImage source;
            try (ImageInputStream imageInput = ImageIO.createImageInputStream(new ByteArrayInputStream(file))) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
                if (!readers.hasNext()) throw new AppException(ErrorCode.AVATAR_INVALID);
                ImageReader reader = readers.next();
                try {
                    reader.setInput(imageInput, true, true);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (width <= 0 || height <= 0 || (long) width * height > MAX_DECODED_PIXELS) {
                        throw new AppException(ErrorCode.AVATAR_INVALID);
                    }
                    source = reader.read(0);
                } finally {
                    reader.dispose();
                }
            }
            int side = Math.min(source.getWidth(), source.getHeight());
            int x = (source.getWidth() - side) / 2;
            int y = (source.getHeight() - side) / 2;
            BufferedImage output = new BufferedImage(OUTPUT_SIZE, OUTPUT_SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = output.createGraphics();
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, OUTPUT_SIZE, OUTPUT_SIZE);
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE, x, y, x + side, y + side, null);
            graphics.dispose();

            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
            if (!writers.hasNext()) throw new AppException(ErrorCode.AVATAR_INVALID);
            ImageWriter writer = writers.next();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ImageOutputStream imageOutput = ImageIO.createImageOutputStream(bytes)) {
                writer.setOutput(imageOutput);
                ImageWriteParam params = writer.getDefaultWriteParam();
                params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                params.setCompressionQuality(0.88f);
                writer.write(null, new IIOImage(output, null, null), params);
            } finally {
                writer.dispose();
            }
            return bytes.toByteArray();
        } catch (AppException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw new AppException(ErrorCode.AVATAR_INVALID, ex);
        }
    }

    private boolean isJpeg(byte[] bytes) {
        return bytes.length >= 3 && (bytes[0] & 0xff) == 0xff
                && (bytes[1] & 0xff) == 0xd8 && (bytes[2] & 0xff) == 0xff;
    }

    private boolean isPng(byte[] bytes) {
        byte[] signature = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
        if (bytes.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) {
            if (bytes[i] != signature[i]) return false;
        }
        return true;
    }
}
