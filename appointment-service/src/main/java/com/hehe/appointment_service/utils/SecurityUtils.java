package com.hehe.appointment_service.utils;

import com.hehe.appointment_service.exception.AppException;
import com.hehe.appointment_service.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;

public class SecurityUtils {

    // Phai doc theo TUNG REQUEST. Neu cache vao static field, moi request sau
    // se dung nham danh tinh cua nguoi goi dau tien.
    private static Authentication auth() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    // Lấy token của người đang gọi để chuyển tiếp
    public static String currentToken() {
        Authentication a = auth();
        if (a != null && a.getPrincipal() instanceof Jwt jwt) {
            return jwt.getTokenValue();
        }
        throw new AppException(ErrorCode.FORBIDDEN);
    }

    // Spring luu role duoi dang "ROLE_<ten>" (do KeycloakRoleConverter them tien to)
    public static boolean hasRole(String role) {
        Authentication a = auth();
        if (a == null) return false;
        return a.getAuthorities().stream().anyMatch(
                g -> Objects.equals(g.getAuthority(), "ROLE_" + role)
        );
    }
}
