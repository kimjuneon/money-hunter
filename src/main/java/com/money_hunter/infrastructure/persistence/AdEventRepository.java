package com.money_hunter.infrastructure.persistence;

import java.time.Instant;
import java.util.List;

import com.money_hunter.domain.AdEvent;
import com.money_hunter.domain.AdEventType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdEventRepository extends JpaRepository<AdEvent, Long> {
	long countByOccurredAtAfter(Instant occurredAt);

	long countByOccurredAtGreaterThanEqualAndOccurredAtLessThan(Instant startedAt, Instant endedAt);

	long countByTypeAndOccurredAtAfter(AdEventType type, Instant occurredAt);

	@Query("""
			select a.type as type,
				count(a.id) as eventCount
			from AdEvent a
			where a.occurredAt >= :startedAt
				and a.occurredAt < :endedAt
			group by a.type
			order by count(a.id) desc
			""")
	List<AdEventTypeCount> findTypeCountsBetween(
			@Param("startedAt") Instant startedAt,
			@Param("endedAt") Instant endedAt);

	@Modifying
	@Query(value = """
			delete from ad_events
			where player_id in (
				select id from players where user_key = :userKey
			)
			""", nativeQuery = true)
	int deleteByPlayerUserKey(@Param("userKey") String userKey);

	@Query("""
			select a.player.userKey as userKey,
				count(a.id) as eventCount,
				max(a.occurredAt) as lastOccurredAt
			from AdEvent a
			where a.occurredAt >= :from
			group by a.player.userKey
			having count(a.id) >= :minimumCount
			order by count(a.id) desc, max(a.occurredAt) desc
			""")
	List<PlayerAdEventCount> findPlayerEventCountsSince(
			@Param("from") Instant from,
			@Param("minimumCount") long minimumCount,
			Pageable pageable);

	interface PlayerAdEventCount {
		String getUserKey();

		long getEventCount();

		Instant getLastOccurredAt();
	}

	interface AdEventTypeCount {
		AdEventType getType();

		long getEventCount();
	}
}
