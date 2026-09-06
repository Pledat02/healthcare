package com.hehe.appointment_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Lay token service-account (client_credentials) de goi API NOI BO cua service khac
 * (vd /api/patients/batch) - KHONG dung token cua user dang dang nhap.
 * Nho vay quyen truy cap noi bo tach roi voi quyen cua bac si/benh nhan.
 * Cache token cho toi gan het han de khong xin lai moi request.
 */
@Component
public class InternalTokenClient {

    private final RestClient http;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    private volatile String cachedToken;
    private volatile Instant expiresAt = Instant.EPOCH;

    public InternalTokenClient(
            @Value("${keycloak.server-url}") String serverUrl,
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.internal-client-id}") String clientId,
            @Value("${keycloak.internal-client-secret}") String clientSecret) {
        // Timeout de khong treo khi Keycloak cham/chet (lay service-account token)
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(2));
        factory.setReadTimeout(Duration.ofSeconds(3));
        this.http = RestClient.builder().baseUrl(serverUrl).requestFactory(factory).build();
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public synchronized String token() {
        if (cachedToken != null && Instant.now().isBefore(expiresAt.minusSeconds(30))) {
            return cachedToken;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        Map<?, ?> res = http.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        if (res == null || res.get("access_token") == null) {
            throw new IllegalStateException("Khong lay duoc service-account token");
        }
        cachedToken = (String) res.get("access_token");
        long expiresIn = ((Number) res.get("expires_in")).longValue();
        expiresAt = Instant.now().plusSeconds(expiresIn);
        return cachedToken;
    }
}
