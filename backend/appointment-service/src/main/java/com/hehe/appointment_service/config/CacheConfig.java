package com.hehe.appointment_service.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Bat Spring Cache (backend Redis, cau hinh o application.yaml: spring.cache.redis.*).
 * Serializer JDK mac dinh cua Spring -> DoctorDto phai implements Serializable.
 *
 * Cache "doctor" (key = doctorId): ho so bac si doc rat nhieu khi dat lich va lam giau du lieu,
 * doi rat it. appointment-service khong so huu du lieu nay nen khong evict duoc khi doctor-service
 * cap nhat -> chap nhan stale toi da bang TTL (5 phut). Chi cache getDoctor(id), KHONG cache getMe()
 * (phu thuoc user) hay getLeaveDates()/batch (can tuoi moi cho nghiep vu chan trung lich).
 */
@Configuration
@EnableCaching
public class CacheConfig {
}
