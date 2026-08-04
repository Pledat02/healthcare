package com.hehe.doctor_service.service;

import com.hehe.doctor_service.Config.DoctorAvatarProperties;
import com.hehe.doctor_service.entity.Doctor;
import com.hehe.doctor_service.entity.DoctorAvatarStatus;
import com.hehe.doctor_service.repository.DoctorRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorAvatarServiceTest {
    @Mock DoctorRepository doctorRepository;
    @Mock SupabaseAvatarStorage storage;

    @Test
    void adminUpload_cropsReencodesAndApproves() throws Exception {
        DoctorAvatarProperties properties = new DoctorAvatarProperties();
        properties.setMaxBytes(5L * 1024 * 1024);
        DoctorAvatarService service = new DoctorAvatarService(doctorRepository, storage, properties);
        Doctor doctor = new Doctor();
        doctor.setId("doctor-1");
        when(doctorRepository.findById("doctor-1")).thenReturn(Optional.of(doctor));
        when(doctorRepository.saveAndFlush(doctor)).thenReturn(doctor);

        BufferedImage source = new BufferedImage(900, 600, BufferedImage.TYPE_INT_RGB);
        var graphics = source.createGraphics();
        graphics.setColor(Color.CYAN);
        graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        graphics.dispose();
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(source, "png", png);

        service.uploadApproved("doctor-1", new MockMultipartFile(
                "file", "portrait.png", "image/png", png.toByteArray()));

        ArgumentCaptor<byte[]> stored = ArgumentCaptor.forClass(byte[].class);
        verify(storage).uploadJpeg(org.mockito.ArgumentMatchers.startsWith("doctors/doctor-1/"), stored.capture());
        BufferedImage normalized = ImageIO.read(new ByteArrayInputStream(stored.getValue()));
        assertThat(normalized.getWidth()).isEqualTo(512);
        assertThat(normalized.getHeight()).isEqualTo(512);
        assertThat(doctor.getAvatarStatus()).isEqualTo(DoctorAvatarStatus.APPROVED);
        assertThat(doctor.getPendingAvatarPath()).isNull();
        assertThat(doctor.getAvatarPath()).endsWith(".jpg");
    }
}
