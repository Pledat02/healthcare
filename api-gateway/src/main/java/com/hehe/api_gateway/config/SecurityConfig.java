package com.hehe.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Gateway la Resource Server (reactive) - kiem tra JWT Keycloak truoc khi dinh tuyen.
 * Thieu/sai token -> 401 ngay tai cong, khong dua toi service con.
 * Phan quyen chi tiet (role, chu so huu) van do tung service con tu lo.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(ex -> ex
                        // Swagger cua cac service con (neu truy cap qua gateway)
                        .pathMatchers("/api/*/v3/api-docs/**", "/swagger-ui/**").permitAll()
                        // Con lai: phai co token hop le
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(org.springframework.security.config.Customizer.withDefaults()));
        return http.build();
    }
}
