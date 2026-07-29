package com.hehe.notification_service.service;

import com.hehe.notification_service.dto.event.AppointmentNotificationEvent;
import com.hehe.notification_service.dto.event.NotificationType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateBuilderTest {

    private final EmailTemplateBuilder builder = new EmailTemplateBuilder();

    @Test
    void escapesUserProvidedHtml() {
        AppointmentNotificationEvent event = AppointmentNotificationEvent.builder()
                .type(NotificationType.APPOINTMENT_CREATED)
                .patientName("<script>alert(1)</script>")
                .doctorName("BS. An")
                .specialization("Tim mạch")
                .appointmentTime(Instant.parse("2026-08-01T02:00:00Z"))
                .reason("<b>đau ngực</b>")
                .build();

        String html = builder.build(event);

        assertThat(html).doesNotContain("<script>", "<b>đau ngực</b>")
                .contains("&lt;script&gt;", "&lt;b&gt;đau ngực&lt;/b&gt;");
    }
}
