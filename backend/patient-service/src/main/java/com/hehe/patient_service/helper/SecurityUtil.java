package com.hehe.patient_service.helper;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;

public class SecurityUtil {
    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(
                grantedAuthority -> Objects.equals(grantedAuthority.getAuthority(), "ROLE_" + role));
    }

    public static boolean isAdmin() {
        return hasRole("ADMIN");
    }
    public static String getCurrentKeyCloakId(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = null;
        if (auth != null) {
            jwt = (Jwt) auth.getPrincipal();
        }
        assert jwt != null;
        return jwt.getSubject();
    }
}
