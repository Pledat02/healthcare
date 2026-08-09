package com.hehe.doctor_service.service;

import com.hehe.doctor_service.config.DoctorAvatarProperties;
import com.hehe.doctor_service.exception.AppException;
import com.hehe.doctor_service.exception.ErrorCode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

@Service
public class SupabaseAvatarStorage {
    private final DoctorAvatarProperties properties;
    private final RestClient client;

    public SupabaseAvatarStorage(DoctorAvatarProperties properties) {
        this.properties = properties;
        RestClient.Builder builder = RestClient.builder();
        if (properties.getSupabaseUrl() != null && !properties.getSupabaseUrl().isBlank()) {
            builder.baseUrl(stripTrailingSlash(properties.getSupabaseUrl()) + "/storage/v1");
        }
        if (properties.getServiceRoleKey() != null && !properties.getServiceRoleKey().isBlank()) {
            String adminKey = properties.getServiceRoleKey();
            // Hosted Storage requires both headers. For sb_secret_* keys Supabase accepts
            // Authorization only when its value exactly matches the apikey header.
            builder.defaultHeader("apikey", adminKey)
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + adminKey);
        }
        this.client = builder.build();
    }

    public void ensurePublicBucket() {
        requireConfigured();
        try {
            client.get().uri("/bucket/{bucket}", properties.getBucket()).retrieve().toBodilessEntity();
        } catch (RestClientResponseException ex) {
            boolean missingBucket = ex.getStatusCode().value() == 404
                    || (ex.getStatusCode().value() == 400
                    && ex.getResponseBodyAsString().contains("NoSuchBucket"));
            if (!missingBucket) throw storageFailure(ex);
            try {
                client.post().uri("/bucket")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(Map.of(
                                "id", properties.getBucket(),
                                "name", properties.getBucket(),
                                "public", true,
                                "file_size_limit", properties.getMaxBytes(),
                                "allowed_mime_types", List.of("image/jpeg")
                        ))
                        .retrieve().toBodilessEntity();
            } catch (RestClientResponseException createError) {
                if (createError.getStatusCode().value() != 409) throw storageFailure(createError);
            }
        }
    }

    public void uploadJpeg(String objectPath, byte[] bytes) {
        ensurePublicBucket();
        try {
            client.post().uri(builder -> builder.path("/object/")
                            .pathSegment(properties.getBucket()).path("/").path(objectPath).build())
                    .contentType(MediaType.IMAGE_JPEG)
                    .header("cache-control", "31536000")
                    .header("x-upsert", "false")
                    .body(bytes)
                    .retrieve().toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw storageFailure(ex);
        }
    }

    public void deleteQuietly(String objectPath) {
        if (objectPath == null || objectPath.isBlank() || !properties.isConfigured()) return;
        try {
            client.delete().uri(builder -> builder.path("/object/")
                            .pathSegment(properties.getBucket()).path("/").path(objectPath).build())
                    .retrieve().toBodilessEntity();
        } catch (RuntimeException ignored) {
            // Don rac Storage se duoc xu ly boi job bao tri; khong lam hong giao dich DB.
        }
    }

    public String publicUrl(String objectPath) {
        if (objectPath == null || objectPath.isBlank() || properties.getSupabaseUrl() == null) return null;
        return stripTrailingSlash(properties.getSupabaseUrl()) + "/storage/v1/object/public/"
                + properties.getBucket() + "/" + objectPath;
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) throw new AppException(ErrorCode.AVATAR_STORAGE_UNAVAILABLE);
    }

    private AppException storageFailure(Exception ex) {
        return new AppException(ErrorCode.AVATAR_STORAGE_UNAVAILABLE, ex);
    }

    private static String stripTrailingSlash(String value) {
        return value == null ? null : value.replaceFirst("/+$", "");
    }
}
