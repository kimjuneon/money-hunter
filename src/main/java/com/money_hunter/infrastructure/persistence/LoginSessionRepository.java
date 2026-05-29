package com.money_hunter.infrastructure.persistence;

import java.util.Optional;

import com.money_hunter.domain.LoginSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginSessionRepository extends JpaRepository<LoginSession, Long> {
	Optional<LoginSession> findByTokenHash(String tokenHash);

	long deleteByUserKey(String userKey);
}
