package com.money_hunter.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;

import com.money_hunter.domain.AdRewardSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdRewardSessionRepository extends JpaRepository<AdRewardSession, Long> {
	Optional<AdRewardSession> findBySessionToken(String sessionToken);

	long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant startedAt, Instant endedAt);

	long countByCreatedAtGreaterThanEqualAndCreatedAtLessThanAndCompletedAtIsNotNull(
			Instant startedAt,
			Instant endedAt);

	@Modifying
	@Query(value = """
			delete from ad_reward_sessions
			where player_id in (
				select id from players where user_key = :userKey
			)
			""", nativeQuery = true)
	int deleteByPlayerUserKey(@Param("userKey") String userKey);
}
