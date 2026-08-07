package com.hehe.security_service.repository;

import com.hehe.security_service.entity.SecurityAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecurityAuditRepository extends JpaRepository<SecurityAudit, String> {
    Page<SecurityAudit> findAllByOrderByOccurredAtDesc(Pageable pageable);
}
