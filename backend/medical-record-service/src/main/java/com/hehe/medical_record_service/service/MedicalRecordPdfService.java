package com.hehe.medical_record_service.service;

import com.hehe.medical_record_service.client.DoctorClient;
import com.hehe.medical_record_service.client.PatientClient;
import com.hehe.medical_record_service.dto.response.DoctorDto;
import com.hehe.medical_record_service.dto.response.MedicalRecordResponse;
import com.hehe.medical_record_service.dto.response.PatientDto;
import com.hehe.medical_record_service.dto.response.PrescriptionItemResponse;
import com.hehe.medical_record_service.exception.AppException;
import com.hehe.medical_record_service.exception.ErrorCode;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Xuat ho so kham + don thuoc ra PDF (BL quick-win).
 * Font Unicode cho tieng Viet: nap tu OS luc runtime (khong commit binary vao repo).
 *  - Windows dev: C:/Windows/Fonts/arial.ttf
 *  - Docker (Linux): /usr/share/fonts/.../DejaVuSans.ttf (Dockerfile cai fonts-dejavu-core)
 *  - Co the ep qua property pdf.font-path.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicalRecordPdfService {

    static final ZoneId CLINIC_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    static final Color BRAND = new Color(37, 99, 235);
    static final String[] FALLBACK_FONTS = {
            "C:/Windows/Fonts/arial.ttf",
            "C:/Windows/Fonts/tahoma.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
            "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
            "/Library/Fonts/Arial.ttf",
            "/System/Library/Fonts/Supplemental/Arial.ttf",
    };

    MedicalRecordService medicalRecordService;
    PatientClient patientClient;
    DoctorClient doctorClient;

    @Value("${pdf.font-path:}")
    @lombok.experimental.NonFinal
    String fontPath;

    public byte[] export(String recordId) {
        // Tai lai ho so QUA getOne -> tan dung kiem tra quyen (ADMIN / chu ho so / bac si dieu tri)
        MedicalRecordResponse mr = medicalRecordService.getOne(recordId);
        PatientDto patient = safe(() -> patientClient.getPatient(mr.getPatientId()));
        DoctorDto doctor = safe(() -> doctorClient.getDoctor(mr.getDoctorId()));

        BaseFont bf = loadUnicodeFont();
        Font h1 = new Font(bf, 16, Font.BOLD, BRAND);
        Font h2 = new Font(bf, 12, Font.BOLD);
        Font label = new Font(bf, 11, Font.BOLD);
        Font normal = new Font(bf, 11);
        Font small = new Font(bf, 9, Font.ITALIC, Color.GRAY);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 48, 48, 54, 40);
            PdfWriter.getInstance(doc, out);
            doc.open();

            Paragraph title = new Paragraph("HỒ SƠ KHÁM BỆNH", h1);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            Paragraph sub = new Paragraph("MediBook - Hệ thống đặt lịch khám", small);
            sub.setAlignment(Element.ALIGN_CENTER);
            sub.setSpacingAfter(14);
            doc.add(sub);

            // Thong tin benh nhan & bac si
            doc.add(section("Thông tin bệnh nhân", h2));
            doc.add(kv("Họ tên", patient != null ? patient.getFullName() : mr.getPatientId(), label, normal));
            if (patient != null) {
                if (patient.getDateOfBirth() != null)
                    doc.add(kv("Ngày sinh", patient.getDateOfBirth().format(D), label, normal));
                if (patient.getGender() != null)
                    doc.add(kv("Giới tính", patient.getGender().toString(), label, normal));
                if (patient.getPhone() != null) doc.add(kv("Điện thoại", patient.getPhone(), label, normal));
            }

            doc.add(section("Thông tin khám", h2));
            doc.add(kv("Bác sĩ", doctor != null ? doctor.getFullName() : mr.getDoctorId(), label, normal));
            if (doctor != null && doctor.getSpecialization() != null)
                doc.add(kv("Chuyên khoa", doctor.getSpecialization(), label, normal));
            if (mr.getCreatedAt() != null)
                doc.add(kv("Ngày khám", mr.getCreatedAt().atZone(CLINIC_ZONE).format(DT), label, normal));
            doc.add(kv("Chẩn đoán", nz(mr.getDiagnosis()), label, normal));
            if (mr.getNotes() != null && !mr.getNotes().isBlank())
                doc.add(kv("Ghi chú", mr.getNotes(), label, normal));

            // Don thuoc
            doc.add(section("Đơn thuốc", h2));
            List<PrescriptionItemResponse> items = mr.getPrescriptionItems();
            if (items == null || items.isEmpty()) {
                doc.add(new Paragraph("Không có đơn thuốc.", normal));
            } else {
                PdfPTable table = new PdfPTable(new float[]{1.2f, 5f, 3f, 2f, 5f});
                table.setWidthPercentage(100);
                table.setSpacingBefore(6);
                for (String head : new String[]{"STT", "Tên thuốc", "Liều", "SL", "Cách dùng"}) {
                    PdfPCell c = new PdfPCell(new Phrase(head, new Font(bf, 10, Font.BOLD, Color.WHITE)));
                    c.setBackgroundColor(BRAND);
                    c.setPadding(6);
                    table.addCell(c);
                }
                int i = 1;
                for (PrescriptionItemResponse it : items) {
                    table.addCell(cell(String.valueOf(i++), normal, bf));
                    table.addCell(cell(nz(it.getMedicineName()), normal, bf));
                    table.addCell(cell(nz(it.getDosage()), normal, bf));
                    table.addCell(cell(String.valueOf(it.getQuantity()), normal, bf));
                    table.addCell(cell(nz(it.getInstruction()), normal, bf));
                }
                doc.add(table);
            }

            Paragraph foot = new Paragraph(
                    "Xuất ngày " + java.time.Instant.now().atZone(CLINIC_ZONE).format(DT)
                            + " · Mã hồ sơ: " + mr.getId(), small);
            foot.setSpacingBefore(20);
            doc.add(foot);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Loi tao PDF ho so {}: {}", recordId, e.getMessage(), e);
            throw new AppException(ErrorCode.PDF_GENERATION_FAILED);
        }
    }

    private BaseFont loadUnicodeFont() {
        List<String> candidates = new ArrayList<>();
        if (fontPath != null && !fontPath.isBlank()) candidates.add(fontPath);
        candidates.addAll(List.of(FALLBACK_FONTS));
        for (String p : candidates) {
            try {
                if (Files.exists(Path.of(p))) {
                    return BaseFont.createFont(p, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                }
            } catch (Exception ignored) {
                // thu font tiep theo
            }
        }
        log.error("Khong tim thay font Unicode nao trong: {}", candidates);
        throw new AppException(ErrorCode.PDF_GENERATION_FAILED);
    }

    // ----- helper trinh bay -----
    private Paragraph section(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setSpacingBefore(12);
        p.setSpacingAfter(4);
        return p;
    }

    private Paragraph kv(String key, String value, Font label, Font normal) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(key + ": ", label));
        p.add(new Chunk(nz(value), normal));
        p.setSpacingAfter(2);
        return p;
    }

    private PdfPCell cell(String text, Font font, BaseFont bf) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setPadding(5);
        return c;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private <T> T safe(java.util.concurrent.Callable<T> call) {
        try {
            return call.call();
        } catch (Exception e) {
            log.warn("Khong lay duoc thong tin phu cho PDF: {}", e.getMessage());
            return null;
        }
    }
}
