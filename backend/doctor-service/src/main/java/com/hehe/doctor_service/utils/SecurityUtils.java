package com.hehe.doctor_service.utils;

import com.hehe.doctor_service.entity.Doctor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;
import java.util.Objects;

public class SecurityUtils {
    public static  boolean isAdmin(){
        Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>)
                SecurityContextHolder.getContext().getAuthentication().getAuthorities();
        return authorities.stream().anyMatch(a -> Objects.equals(a.getAuthority(), "ROLE_ADMIN"));
    }

    public static String getKeyCloakId(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Jwt jwt = (Jwt) auth.getPrincipal();
        assert jwt != null;
        return jwt.getSubject();
    }
    public static boolean isAccessed (Doctor doctor){
        return isAdmin() || doctor.getKeycloakId().equals(getKeyCloakId());
    }
}
