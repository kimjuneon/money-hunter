package com.money_hunter.infrastructure.persistence;

import com.money_hunter.domain.AdminAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLog, Long> {
	Page<AdminAuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
