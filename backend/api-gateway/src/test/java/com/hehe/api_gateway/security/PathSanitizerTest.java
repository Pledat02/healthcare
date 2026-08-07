package com.hehe.api_gateway.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PathSanitizerTest {
    private final PathSanitizer sanitizer = new PathSanitizer();

    @Test
    void removesIdentifiersFromStoredPath() {
        assertThat(sanitizer.normalize("/api/doctors/12345"))
                .isEqualTo("/api/doctors/{id}");
    }

    @Test
    void detectsCommonSecretScanningPaths() {
        assertThat(sanitizer.isSuspicious("/.env")).isTrue();
        assertThat(sanitizer.isSuspicious("/api/doctors")).isFalse();
    }
}
