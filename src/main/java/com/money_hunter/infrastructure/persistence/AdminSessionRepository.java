package com.money_hunter.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;

import com.money_hunter.domain.AdminSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdminSessionRepository extends JpaRepository<AdminSession, Long> {
	Optional<AdminSession> findByTokenHash(String tokenHash);

	long deleteByExpiresAtBefore(Instant expiresAt);
}
