package com.hehe.security_service.repository;

import com.hehe.security_service.entity.SecurityAlert;
import com.hehe.security_service.model.AlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.Optional;

public interface SecurityAlertRepository extends JpaRepository<SecurityAlert, String> {
    Page<SecurityAlert> findByStatus(AlertStatus status, Pageable pageable);
    long countByStatusIn(Collection<AlertStatus> statuses);
    Optional<SecurityAlert> findFirstByClientIpAndTypeAndStatusInAndLastSeenAtAfterOrderByLastSeenAtDesc(
            String clientIp, String type, Collection<AlertStatus> statuses, Instant cutoff);
}
