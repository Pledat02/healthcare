package com.hehe.notification_service.service;

import com.hehe.notification_service.dto.event.AppointmentNotificationEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class EmailTemplateBuilder {

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy")
                    .withZone(ZoneId.of("Asia/Ho_Chi_Minh"));

    public String build(AppointmentNotificationEvent e) {
        return switch (e.getType()) {
            case APPOINTMENT_CREATED -> card(e,
                    "Đặt lịch khám thành công", "#2a7ae2",
                    "Lịch khám của bạn đã được ghi nhận:",
                    "Vui lòng đến trước giờ hẹn 15 phút. Nếu cần hủy, hãy thao tác trên hệ thống.");
            case APPOINTMENT_RESCHEDULED -> card(e,
                    "Lịch khám đã được thay đổi", "#0d9488",
                    "Thông tin lịch khám mới của bạn:",
                    "Email nhắc lịch sẽ được gửi theo thời gian mới.");
            case APPOINTMENT_CONFIRMED -> card(e,
                    "Lịch khám đã được xác nhận", "#2e7d32",
                    "Bác sĩ đã xác nhận lịch khám của bạn:",
                    "Hẹn gặp bạn đúng giờ.");
            case APPOINTMENT_CANCELLED -> card(e,
                    "Lịch khám đã bị hủy", "#c62828",
                    "Lịch khám sau đây đã được hủy:",
                    (e.getCancelReason() == null || e.getCancelReason().isBlank())
                            ? "Bạn có thể đặt lại bất cứ lúc nào."
                            : "Lý do hủy: " + e.getCancelReason() + ". Bạn có thể đặt lại bất cứ lúc nào.");
            case APPOINTMENT_COMPLETED -> card(e,
                    "Cảm ơn bạn đã đến khám", "#6a1b9a",
                    "Buổi khám đã hoàn tất:",
                    "Bạn có thể xem kết quả khám và đơn thuốc trên hệ thống.");
            // US-09: mail nhac truoc 24h do job tu gui
            case REMINDER -> card(e,
                    "Nhắc lịch khám ngày mai", "#d97706",
                    "Bạn có lịch khám vào ngày mai:",
                    "Vui lòng đến trước giờ hẹn 15 phút. Nếu bận, hãy hủy lịch trên hệ thống để nhường chỗ cho người khác.");
        };
    }

    private String card(AppointmentNotificationEvent e, String title, String color,
                        String intro, String footerNote) {
        String time = e.getAppointmentTime() == null ? "—" : FMT.format(e.getAppointmentTime());
        return """
        <div style="font-family: Arial, sans-serif; max-width: 500px; margin: auto;
                    border: 1px solid #e0e0e0; border-radius: 8px; padding: 24px;">
          <h2 style="color: %s;">%s</h2>
          <p>Xin chào <b>%s</b>,</p>
          <p>%s</p>
          <table style="width:100%%; border-collapse: collapse;">
            <tr><td style="padding:6px 0;">️ Bác sĩ</td><td><b>%s</b></td></tr>
            <tr><td style="padding:6px 0;"> Chuyên khoa</td><td>%s</td></tr>
            <tr><td style="padding:6px 0;"> Thời gian</td><td><b>%s</b></td></tr>
            <tr><td style="padding:6px 0;"> Lý do khám</td><td>%s</td></tr>
          </table>
          <p style="margin-top:16px; color:#666;">%s</p>
          <hr style="border:none; border-top:1px solid #eee;">
          <p style="font-size:12px; color:#999;">Email tự động — vui lòng không trả lời.</p>
        </div>
        """.formatted(color, escape(title), escape(e.getPatientName()), escape(intro),
                escape(e.getDoctorName()), escape(e.getSpecialization()), escape(time),
                escape(e.getReason()), escape(footerNote));
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null || value.isBlank() ? "—" : value);
    }
}
