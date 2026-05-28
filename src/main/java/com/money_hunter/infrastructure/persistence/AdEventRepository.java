package com.money_hunter.infrastructure.persistence;

import java.time.Instant;
import java.util.List;

import com.money_hunter.domain.AdEvent;
import com.money_hunter.domain.AdEventType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdEventRepository extends JpaRepository<AdEvent, Long> {
	long countByOccurredAtAfter(Instant occurredAt);

	long countByTypeAndOccurredAtAfter(AdEventType type, Instant occurredAt);

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
}
