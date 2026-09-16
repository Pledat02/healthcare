package com.hehe.medical_record_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * BL-03: moi RestClient goi service khac PHAI co connect/read timeout.
 * Neu khong, downstream treo -> thread cua service nay treo vo han -> can kiet thread pool.
 */
@Configuration
public class RestClientConfig {

    private ClientHttpRequestFactory timeoutFactory(long connectMs, long readMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectMs));
        factory.setReadTimeout(Duration.ofMillis(readMs));
        return factory;
    }

    @Bean
    RestClient doctorRestClient(
            @Value("${services.doctor.url}") String baseUrl,
            @Value("${services.http.connect-timeout-ms:2000}") long connectMs,
            @Value("${services.http.read-timeout-ms:3000}") long readMs) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(timeoutFactory(connectMs, readMs))
                .build();
    }

    @Bean
    RestClient patientRestClient(
            @Value("${services.patient.url}") String baseUrl,
            @Value("${services.http.connect-timeout-ms:2000}") long connectMs,
            @Value("${services.http.read-timeout-ms:3000}") long readMs) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(timeoutFactory(connectMs, readMs))
                .build();
    }

    @Bean
    RestClient appointmentRestClient(
            @Value("${services.appointment.url}") String baseUrl,
            @Value("${services.http.connect-timeout-ms:2000}") long connectMs,
            @Value("${services.http.read-timeout-ms:3000}") long readMs) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(timeoutFactory(connectMs, readMs))
                .build();
    }
}
