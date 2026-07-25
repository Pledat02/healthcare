package com.hehe.notification_service.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Async   // gửi ở luồng riêng - không bắt người đặt lịch phải chờ
    public void sendHtml(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);   // true = nội dung là HTML
            mailSender.send(message);
            log.info("Đã gửi mail tới {}", to);
        } catch (Exception e) {
            // US mục 6: gửi mail lỗi KHÔNG được làm sập luồng chính -> chỉ log
            log.error("Gửi mail tới {} thất bại: {}", to, e.getMessage());
        }
    }
}