package com.hehe.doctor_service.client;

import com.hehe.doctor_service.exception.AppException;
import com.hehe.doctor_service.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * Goi Keycloak Admin API bang SERVICE ACCOUNT (client credentials).
 * Khong muon token cua admin dang dang nhap: tai khoan ADMIN trong realm
 * khong co quyen quan tri Keycloak, chi service account moi co manage-users.
 */
@Component
@Slf4j
public class KeycloakAdminClient {

    private final RestClient http;
    private final String realm;
    private final String clientId;
    private final String clientSecret;

    public KeycloakAdminClient(
            @Value("${keycloak.server-url}") String serverUrl,
            @Value("${keycloak.realm}") String realm,
            @Value("${keycloak.admin-client-id}") String clientId,
            @Value("${keycloak.admin-client-secret}") String clientSecret) {
        this.http = RestClient.builder().baseUrl(serverUrl).build();
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /** Lay access token cua service account */
    private String serviceToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        Map<String, Object> res = http.post()
                .uri("/realms/{realm}/protocol/openid-connect/token", realm)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (res == null || res.get("access_token") == null) {
            throw new AppException(ErrorCode.KEYCLOAK_UNAVAILABLE);
        }
        return (String) res.get("access_token");
    }

    /**
     * Tao user Keycloak cho bac si va gan role DOCTOR.
     * @return id (sub) cua user vua tao -> luu vao Doctor.keycloakId
     */
    public String createDoctorUser(String username, String password, String fullName, String email) {
        String token = serviceToken();
        String firstName = fullName;
        String lastName = "";
        int lastSpace = fullName.trim().lastIndexOf(' ');
        if (lastSpace > 0) {
            firstName = fullName.substring(0, lastSpace);
            lastName = fullName.substring(lastSpace + 1);
        }

        Map<String, Object> body = Map.of(
                "username", username,
                "enabled", true,
                "emailVerified", true,
                "firstName", firstName,
                "lastName", lastName,
                "email", email == null ? "" : email,
                "credentials", List.of(Map.of(
                        "type", "password",
                        "value", password,
                        "temporary", false)));

        URI location;
        try {
            location = http.post()
                    .uri("/admin/realms/{realm}/users", realm)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(s -> s.value() == 409, (req, resp) -> {
                        throw new AppException(ErrorCode.USERNAME_ALREADY_EXISTS);
                    })
                    .toBodilessEntity()
                    .getHeaders()
                    .getLocation();
        } catch (AppException e) {
            throw e;
        } catch (Exception e) {
            log.error("Tao user Keycloak that bai: {}", e.getMessage());
            throw new AppException(ErrorCode.KEYCLOAK_UNAVAILABLE);
        }

        if (location == null) {
            throw new AppException(ErrorCode.KEYCLOAK_UNAVAILABLE);
        }
        // Location: .../users/{id}
        String userId = location.getPath().substring(location.getPath().lastIndexOf('/') + 1);

        try {
            assignDoctorRole(token, userId);
        } catch (Exception e) {
            // User da tao nhung khong gan duoc role -> xoa di, tranh tai khoan mo coi
            log.error("Gan role DOCTOR that bai, rollback user {}: {}", userId, e.getMessage());
            deleteUser(userId);
            throw new AppException(ErrorCode.KEYCLOAK_UNAVAILABLE);
        }
        return userId;
    }

    /**
     * Gan role DOCTOR.
     * Luu y: tai khoan van mang them PATIENT vi day la role mac dinh cua realm
     * (nam trong composite default-roles-healthcare, phuc vu benh nhan tu dang ky)
     * nen khong go rieng ra duoc. Vi vay he thong xac dinh vai tro theo thu tu
     * uu tien ADMIN > DOCTOR > PATIENT, khong dua vao "co role PATIENT hay khong".
     */
    private void assignDoctorRole(String token, String userId) {
        http.post()
                .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", realm, userId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(realmRole(token, "DOCTOR")))
                .retrieve()
                .toBodilessEntity();
    }

    private Map<String, Object> realmRole(String token, String name) {
        return http.get()
                .uri("/admin/realms/{realm}/roles/{name}", realm, name)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    /** Xoa user Keycloak - dung de rollback khi luu DB that bai, hoac khi xoa bac si */
    public void deleteUser(String keycloakId) {
        if (keycloakId == null || keycloakId.isBlank()) return;
        try {
            http.delete()
                    .uri("/admin/realms/{realm}/users/{id}", realm, keycloakId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken())
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            // Khong lam hong luong chinh; chi ghi log de xu ly thu cong
            log.error("Xoa user Keycloak {} that bai: {}", keycloakId, e.getMessage());
        }
    }
}
