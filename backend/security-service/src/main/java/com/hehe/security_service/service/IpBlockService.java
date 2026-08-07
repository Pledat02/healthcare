package com.hehe.security_service.service;

import com.hehe.security_service.dto.CreateIpBlockRequest;
import com.hehe.security_service.entity.IpBlock;
import com.hehe.security_service.repository.IpBlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IpBlockService {
    public static final String BLOCK_PREFIX = "security:block:ip:";

    private final IpBlockRepository repository;
    private final StringRedisTemplate redis;
    private final IpAddressService ipAddressService;
    private final SecurityAuditService auditService;

    public List<IpBlock> list() {
        expireStaleRows();
        return repository.findAllByOrderByBlockedAtDesc();
    }

    @Transactional
    public IpBlock block(CreateIpBlockRequest request, String actorId, String traceId) {
        String ip = ipAddressService.canonicalize(request.ip());
        if (isLoopback(ip)) {
            throw new IllegalArgumentException("Không được chặn địa chỉ loopback");
        }
        repository.findFirstByIpAddressAndActiveTrueOrderByBlockedAtDesc(ip)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("IP đang nằm trong blocklist");
                });

        Instant now = Instant.now();
        Instant expiresAt = request.durationMinutes() == null
                ? null : now.plus(Duration.ofMinutes(request.durationMinutes()));
        IpBlock block = IpBlock.builder()
                .id(UUID.randomUUID().toString())
                .ipAddress(ip)
                .reason(request.reason().trim())
                .automatic(false)
                .blockedBy(actorId)
                .blockedAt(now)
                .expiresAt(expiresAt)
                .active(true)
                .build();

        writeRedis(block);
        try {
            repository.save(block);
            auditService.record(actorId, "BLOCK_IP", "IP", ip, block.getReason(), traceId);
            return block;
        } catch (RuntimeException exception) {
            redis.delete(BLOCK_PREFIX + ip);
            throw exception;
        }
    }

    @Transactional
    public IpBlock unblock(String id, String actorId, String traceId) {
        IpBlock block = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy block record"));
        if (!block.isActive()) {
            return block;
        }
        redis.delete(BLOCK_PREFIX + block.getIpAddress());
        block.setActive(false);
        block.setUnblockedBy(actorId);
        block.setUnblockedAt(Instant.now());
        repository.save(block);
        auditService.record(actorId, "UNBLOCK_IP", "IP", block.getIpAddress(),
                "Manual unblock", traceId);
        return block;
    }

    private void writeRedis(IpBlock block) {
        String value = block.getId();
        if (block.getExpiresAt() == null) {
            redis.opsForValue().set(BLOCK_PREFIX + block.getIpAddress(), value);
        } else {
            Duration ttl = Duration.between(Instant.now(), block.getExpiresAt());
            redis.opsForValue().set(BLOCK_PREFIX + block.getIpAddress(), value, ttl);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void restoreActiveBlocks() {
        expireStaleRows();
        repository.findByActiveTrue().forEach(this::writeRedis);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void expireActiveBlocks() {
        expireStaleRows();
    }

    private void expireStaleRows() {
        Instant now = Instant.now();
        repository.findByActiveTrueAndExpiresAtBefore(now).stream()
                .forEach(block -> {
                    block.setActive(false);
                    repository.save(block);
                    redis.delete(BLOCK_PREFIX + block.getIpAddress());
                });
    }

    private boolean isLoopback(String ip) {
        return "127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip);
    }
}
