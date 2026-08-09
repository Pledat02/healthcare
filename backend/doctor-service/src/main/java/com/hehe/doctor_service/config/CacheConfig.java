package com.hehe.doctor_service.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Bat cache. Dung serializer JDK mac dinh cua Spring (DoctorResponse implements Serializable),
 * xu ly gon ca List bat bien va LocalTime. TTL lay tu application.yaml (spring.cache.redis.time-to-live).
 * Invalidation bang @CacheEvict trong DoctorService khi admin them/sua/xoa.
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
