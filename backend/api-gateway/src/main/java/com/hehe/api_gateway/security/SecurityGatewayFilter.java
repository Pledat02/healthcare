package com.hehe.api_gateway.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class SecurityGatewayFilter implements WebFilter {
    private static final Logger log = LoggerFactory.getLogger(SecurityGatewayFilter.class);
    private static final String BLOCK_PREFIX = "security:block:ip:";
    private static final String RATE_PREFIX = "security:rate:ip:";

    private final ReactiveStringRedisTemplate redis;
    private final ClientIpResolver clientIpResolver;
    private final PathSanitizer pathSanitizer;
    private final SecurityEventPublisher eventPublisher;

    public SecurityGatewayFilter(ReactiveStringRedisTemplate redis,
                                 ClientIpResolver clientIpResolver,
                                 PathSanitizer pathSanitizer,
                                 SecurityEventPublisher eventPublisher) {
        this.redis = redis;
        this.clientIpResolver = clientIpResolver;
        this.pathSanitizer = pathSanitizer;
        this.eventPublisher = eventPublisher;
    }

    @Value("${security.rate-limit-per-minute:120}")
    private long rateLimitPerMinute;

    @Value("${security.cloudflare.required:false}")
    private boolean cloudflareRequired;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String rawPath = exchange.getRequest().getPath().value();
        if (rawPath.startsWith("/actuator/health")) {
            return chain.filter(exchange);
        }

        String clientIp = clientIpResolver.resolve(exchange);
        long startedNanos = System.nanoTime();
        if (cloudflareRequired && !clientIpResolver.isCloudflareRequest(exchange)) {
            return reject(exchange, HttpStatus.FORBIDDEN,
                    "Direct origin access is not allowed", clientIp, startedNanos,
                    "ORIGIN_BYPASS", 100, "BLOCKED");
        }
        return redis.hasKey(BLOCK_PREFIX + clientIp)
                .onErrorResume(error -> redisFailOpen("block-check", error, false))
                .flatMap(blocked -> {
                    if (Boolean.TRUE.equals(blocked)) {
                        return reject(exchange, HttpStatus.FORBIDDEN,
                                "Truy cập bị từ chối", clientIp, startedNanos,
                                "IP_BLOCKED", 100, "BLOCKED");
                    }
                    return checkRateLimit(exchange, chain, clientIp, startedNanos);
                });
    }

    private Mono<Void> checkRateLimit(ServerWebExchange exchange, WebFilterChain chain,
                                      String clientIp, long startedNanos) {
        long minute = Instant.now().getEpochSecond() / 60;
        String key = RATE_PREFIX + clientIp + ":" + minute;
        return redis.opsForValue().increment(key)
                .flatMap(count -> count == 1
                        ? redis.expire(key, Duration.ofMinutes(2)).thenReturn(count)
                        : Mono.just(count))
                .onErrorResume(error -> redisFailOpen("rate-limit", error, 0L))
                .flatMap(count -> {
                    if (count > rateLimitPerMinute) {
                        return reject(exchange, HttpStatus.TOO_MANY_REQUESTS,
                                "Quá nhiều yêu cầu", clientIp, startedNanos,
                                "RATE_LIMITED", 80, "RATE_LIMITED");
                    }
                    return chain.filter(exchange)
                            .doFinally(signal -> publishCompleted(exchange, clientIp, startedNanos));
                });
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message,
                              String clientIp, long startedNanos, String eventType,
                              int riskScore, String action) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":" + status.value() + ",\"message\":\"" + message + "\",\"data\":null}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        publish(exchange, clientIp, startedNanos, status.value(), eventType, riskScore, action);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private void publishCompleted(ServerWebExchange exchange, String clientIp, long startedNanos) {
        int status = exchange.getResponse().getStatusCode() == null
                ? 500 : exchange.getResponse().getStatusCode().value();
        String rawPath = exchange.getRequest().getPath().value();
        boolean suspicious = pathSanitizer.isSuspicious(rawPath);
        String eventType;
        int risk;
        if (suspicious) {
            eventType = "SUSPICIOUS_PATH";
            risk = 90;
        } else if (status == 401) {
            eventType = "AUTHENTICATION_FAILURE";
            risk = 30;
        } else if (status == 403) {
            eventType = "ACCESS_DENIED";
            risk = 35;
        } else if (status == 404) {
            eventType = "NOT_FOUND";
            risk = 10;
        } else if (status >= 500) {
            eventType = "SERVER_ERROR";
            risk = 50;
        } else {
            eventType = "ACCESS";
            risk = 0;
        }
        long latency = elapsedMillis(startedNanos);
        if (latency >= 5000 && risk < 20) {
            eventType = "SLOW_REQUEST";
            risk = 20;
        }
        publish(exchange, clientIp, startedNanos, status, eventType, risk, "ALLOWED");
    }

    private void publish(ServerWebExchange exchange, String clientIp, long startedNanos,
                         int status, String eventType, int risk, String action) {
        String rawUserAgent = exchange.getRequest().getHeaders().getFirst("User-Agent");
        String userAgent = sanitize(rawUserAgent, 512);
        String traceId = sanitize(exchange.getRequest().getHeaders().getFirst("X-Request-Id"), 64);
        eventPublisher.publish(new SecurityAccessEvent(
                UUID.randomUUID().toString(),
                Instant.now(),
                clientIp,
                exchange.getRequest().getMethod().name(),
                pathSanitizer.normalize(exchange.getRequest().getPath().value()),
                status,
                elapsedMillis(startedNanos),
                userAgent,
                eventType,
                risk,
                action,
                traceId,
                clientIpResolver.cloudflareRayId(exchange)));
    }

    private long elapsedMillis(long startedNanos) {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }

    private String sanitize(String value, int maxLength) {
        if (value == null) return null;
        String clean = value.replace('\r', ' ').replace('\n', ' ');
        return clean.length() <= maxLength ? clean : clean.substring(0, maxLength);
    }

    private <T> Mono<T> redisFailOpen(String operation, Throwable error, T fallback) {
        log.error("Redis security {} unavailable; fail-open: {}", operation, error.getMessage());
        return Mono.just(fallback);
    }
}
