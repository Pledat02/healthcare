package com.hehe.appointment_service.config;

import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity httpSecurity, KeycloakRoleConverter converter){
        // set converter
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(converter);
        // config
        httpSecurity.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers(
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**"
                                ).permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/appointments/statistics").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET, "/api/appointments").hasRole("ADMIN")
                                .requestMatchers(HttpMethod.GET,"/api/appointments/patients/me").hasRole("PATIENT")
                                .requestMatchers(HttpMethod.GET,"/api/appointments/doctors/me").hasRole("DOCTOR")
                                .requestMatchers(HttpMethod.PUT, "/api/appointments/**").hasRole("PATIENT")
                                .requestMatchers(HttpMethod.PATCH, "/api/appointments/*/cancel").hasRole("PATIENT")
                                .requestMatchers(HttpMethod.PATCH, "/api/appointments/*/confirm").hasAnyRole("DOCTOR", "ADMIN")
                                .requestMatchers(HttpMethod.PATCH, "/api/appointments/*/complete").hasRole("DOCTOR")

                                .anyRequest().authenticated()

                )
                .oauth2ResourceServer(
                        o -> o.jwt(
                                j -> j.jwtAuthenticationConverter(jwtAuthenticationConverter))
                );


        return httpSecurity.build();

    }
}
