package com.hehe.appointment_service.config;

import com.hehe.appointment_service.exception.AppException;
import com.hehe.appointment_service.exception.ErrorCode;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * BL-03: bao moi lenh goi REST lien service bang Circuit Breaker + Retry.
 *
 * Nguyen tac:
 *  - Chi RETRY va tinh FAILURE cho loi ha nguon that su: timeout/IO (ResourceAccessException)
 *    va 5xx (HttpServerErrorException). Loi nghiep vu (AppException, vd 404 not-found) duoc
 *    BO QUA -> khong retry, khong lam mo circuit.
 *  - Thu tu boc: CircuitBreaker(Retry(call)). Khi circuit MO thi bao loi ngay, KHONG retry.
 *  - Moi that bai cuoi cung (het retry / circuit mo / timeout) -> AppException 503 xac dinh,
 *    de GlobalExceptionHandler tra ve dung ma 503 (khong treo, khong 500 mo ho).
 */
@Component
public class ResilientCaller {

    private static final Logger log = LoggerFactory.getLogger(ResilientCaller.class);

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    public ResilientCaller(
            @Value("${resilience.retry.max-attempts:3}") int retryMaxAttempts,
            @Value("${resilience.retry.wait-ms:200}") long retryWaitMs,
            @Value("${resilience.circuitbreaker.sliding-window-size:10}") int windowSize,
            @Value("${resilience.circuitbreaker.minimum-number-of-calls:5}") int minCalls,
            @Value("${resilience.circuitbreaker.failure-rate-threshold:50}") float failureRate,
            @Value("${resilience.circuitbreaker.wait-duration-open-ms:10000}") long waitOpenMs,
            @Value("${resilience.circuitbreaker.permitted-calls-half-open:3}") int halfOpenCalls,
            @Value("${resilience.circuitbreaker.slow-call-ms:2000}") long slowCallMs) {

        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(windowSize)
                .minimumNumberOfCalls(minCalls)
                .failureRateThreshold(failureRate)
                .waitDurationInOpenState(Duration.ofMillis(waitOpenMs))
                .permittedNumberOfCallsInHalfOpenState(halfOpenCalls)
                .slowCallDurationThreshold(Duration.ofMillis(slowCallMs))
                .slowCallRateThreshold(100f)
                // Loi nghiep vu khong phai loi ha nguon -> khong lam mo circuit
                .ignoreExceptions(AppException.class)
                .build();
        this.circuitBreakerRegistry = CircuitBreakerRegistry.of(cbConfig);

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(retryMaxAttempts)
                .waitDuration(Duration.ofMillis(retryWaitMs))
                // Chi retry loi thoang qua: timeout/IO va 5xx
                .retryExceptions(ResourceAccessException.class, HttpServerErrorException.class)
                .ignoreExceptions(AppException.class)
                .build();
        this.retryRegistry = RetryRegistry.of(retryConfig);
    }

    /**
     * Goi {@code action} duoi su bao ve cua circuit breaker + retry mang ten {@code name}.
     * @param name          ten instance (vd "doctor-service") - gom metric/trang thai theo downstream
     * @param onUnavailable ma loi 503 tra ve khi downstream khong dung duoc
     */
    public <T> T call(String name, ErrorCode onUnavailable, Supplier<T> action) {
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(name);
        Retry retry = retryRegistry.retry(name);

        Supplier<T> decorated = CircuitBreaker.decorateSupplier(
                circuitBreaker,
                Retry.decorateSupplier(retry, action));

        try {
            return decorated.get();
        } catch (AppException e) {
            throw e;   // loi nghiep vu (vd 404) -> giu nguyen
        } catch (Exception e) {
            log.warn("[{}] downstream loi -> tra 503: {}", name, e.toString());
            throw new AppException(onUnavailable);
        }
    }
}
