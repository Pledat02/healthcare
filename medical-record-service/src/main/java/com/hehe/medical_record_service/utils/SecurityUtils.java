package com.hehe.appointment_service.utils;

import com.hehe.appointment_service.exception.AppException;
import com.hehe.appointment_service.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;

public class SecurityUtils {
    static Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    // Lấy token của người đang gọi để chuyển tiếp
    public static String currentToken() {

        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getTokenValue();
        }
        throw new AppException(ErrorCode.FORBIDDEN);
    }
    public static boolean hasRole(String role){
        return auth.getAuthorities().stream().anyMatch(
                g -> Objects.requireNonNull(g.getAuthority()).equalsIgnoreCase(role)
        );
    }
}
