package com.hehe.api_gateway.security;

import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class PathSanitizer {
    private static final Pattern UUID = Pattern.compile(
            "(?i)^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$");
    private static final Pattern NUMBER = Pattern.compile("^\\d+$");
    private static final Pattern LONG_ID = Pattern.compile("^[A-Za-z0-9_-]{18,}$");
    private static final Set<String> SUSPICIOUS_MARKERS = Set.of(
            "/.env", "/.git", "/wp-admin", "/phpmyadmin", "../", "%2e%2e", "%252e");

    public String normalize(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) return "/";
        String clean = rawPath.replace('\r', ' ').replace('\n', ' ');
        String[] segments = clean.split("/", -1);
        StringBuilder result = new StringBuilder();
        for (String segment : segments) {
            if (segment.isEmpty()) continue;
            result.append('/');
            result.append(isIdentifier(segment) ? "{id}" : limit(segment, 80));
        }
        return result.isEmpty() ? "/" : limit(result.toString(), 512);
    }

    public boolean isSuspicious(String rawPath) {
        String lower = rawPath == null ? "" : rawPath.toLowerCase(Locale.ROOT);
        return SUSPICIOUS_MARKERS.stream().anyMatch(lower::contains);
    }

    private boolean isIdentifier(String segment) {
        return UUID.matcher(segment).matches()
                || NUMBER.matcher(segment).matches()
                || LONG_ID.matcher(segment).matches();
    }

    private String limit(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
