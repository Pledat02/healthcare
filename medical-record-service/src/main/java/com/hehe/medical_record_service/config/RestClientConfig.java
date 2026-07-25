package com.hehe.medical_record_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    RestClient doctorRestClient(@Value("${services.doctor.url}") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
    @Bean
    RestClient patientRestClient(@Value("${services.patient.url") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
    @Bean
    RestClient appointmentRestClient(@Value("${services.appointment.url") String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }
}
