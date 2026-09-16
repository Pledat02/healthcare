package com.hehe.appointment_service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.springboot3.circuitbreaker.autoconfigure.CircuitBreakerAutoConfiguration;
import io.github.resilience4j.springboot3.retry.autoconfigure.RetryAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * THI NGHIEM (khong phai test that): chung minh starter resilience4j-spring-boot3 (build cho Boot 3)
 * KHONG chi nap duoc, ma con INTERCEPT that su annotation @CircuitBreaker + goi fallback tren Boot 4.1.
 * Chay OFFLINE, khong dung DB/Keycloak.
 */
class Resilience4jStarterProbeTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    AopAutoConfiguration.class,
                    CircuitBreakerAutoConfiguration.class,
                    RetryAutoConfiguration.class))
            .withUserConfiguration(ProbeConfig.class)
            .withPropertyValues(
                    "resilience4j.circuitbreaker.instances.probe.sliding-window-size=4",
                    "resilience4j.circuitbreaker.instances.probe.minimum-number-of-calls=4",
                    "resilience4j.circuitbreaker.instances.probe.failure-rate-threshold=50");

    @Test
    void annotationShouldBeInterceptedAndFallbackInvoked() {
        runner.run(ctx -> {
            assertThat(ctx.getStartupFailure()).isNull();
            ProbeService svc = ctx.getBean(ProbeService.class);

            // Method luon nem loi -> neu annotation duoc intercept, fallback tra "FALLBACK"
            String result = svc.alwaysFail();
            assertThat(result)
                    .as("Neu @CircuitBreaker bi bo qua, cho nay se nem RuntimeException thay vi tra fallback")
                    .isEqualTo("FALLBACK");
            System.out.println(">>> PROBE annotation @CircuitBreaker duoc INTERCEPT, fallback chay OK");
        });
    }

    @Configuration
    static class ProbeConfig {
        @Bean
        ProbeService probeService() { return new ProbeService(); }
    }

    static class ProbeService {
        final AtomicInteger calls = new AtomicInteger();

        @CircuitBreaker(name = "probe", fallbackMethod = "fallback")
        public String alwaysFail() {
            calls.incrementAndGet();
            throw new RuntimeException("downstream loi gia lap");
        }

        @SuppressWarnings("unused")
        String fallback(Throwable t) { return "FALLBACK"; }
    }
}
