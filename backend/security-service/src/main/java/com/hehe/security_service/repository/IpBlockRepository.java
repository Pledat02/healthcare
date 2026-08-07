package com.hehe.security_service.repository;

import com.hehe.security_service.entity.IpBlock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.time.Instant;

public interface IpBlockRepository extends JpaRepository<IpBlock, String> {
    List<IpBlock> findAllByOrderByBlockedAtDesc();
    Optional<IpBlock> findFirstByIpAddressAndActiveTrueOrderByBlockedAtDesc(String ipAddress);
    List<IpBlock> findByActiveTrue();
    List<IpBlock> findByActiveTrueAndExpiresAtBefore(Instant cutoff);
    long countByActiveTrue();
}
