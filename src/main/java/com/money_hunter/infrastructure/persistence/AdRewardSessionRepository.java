package com.money_hunter.infrastructure.persistence;

import java.util.Optional;

import com.money_hunter.domain.AdRewardSession;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdRewardSessionRepository extends JpaRepository<AdRewardSession, Long> {
	Optional<AdRewardSession> findBySessionToken(String sessionToken);

	long deleteByPlayerUserKey(String userKey);
}
