package com.hehe.notification_service.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${notification.mail-from}")
    private String mailFrom;

    /** Gửi đồng bộ để lớp gọi có thể ghi nhận chính xác SENT hoặc FAILED. */
    public void sendHtml(String to, String subject, String htmlBody) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(mailFrom, "MediBook");
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlBody, true);   // true = nội dung là HTML
        mailSender.send(message);
        log.info("Đã gửi mail tới {}", to);
    }
}
