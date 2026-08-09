package com.hehe.doctor_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain filterChain(HttpSecurity httpSecurity, KeycloakRoleConverter keycloakRoleConverter) {
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(keycloakRoleConverter);
        httpSecurity.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // Bac si tu quan ly ngay nghi cua minh (khong phai ADMIN)
                        .requestMatchers(HttpMethod.POST, "/api/doctors/me/leaves").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.DELETE, "/api/doctors/me/leaves/**").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.PUT, "/api/doctors/me/avatar").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.GET, "/api/doctors/*/avatar/review").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/doctors/**").authenticated()
                        .requestMatchers("/api/doctors/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(o -> o.jwt(
                        j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter)));
        return httpSecurity.build();
    }

}
