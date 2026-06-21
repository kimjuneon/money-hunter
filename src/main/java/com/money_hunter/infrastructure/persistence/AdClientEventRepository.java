package com.money_hunter.infrastructure.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import com.money_hunter.domain.AdClientEvent;
import com.money_hunter.domain.AdClientEventType;
import com.money_hunter.domain.AdEventType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdClientEventRepository extends JpaRepository<AdClientEvent, Long> {
	@Query("""
			select e.type as type,
				e.eventType as eventType,
				count(e.id) as eventCount
			from AdClientEvent e
			where e.occurredAt >= :startedAt
				and e.occurredAt < :endedAt
				and e.type in :types
			group by e.type, e.eventType
			""")
	List<AdClientEventTypeCount> findTypeEventCountsBetween(
			@Param("startedAt") Instant startedAt,
			@Param("endedAt") Instant endedAt,
			@Param("types") Collection<AdEventType> types);

	@Query("""
			select e.player.userKey as userKey,
				e.player.adminNickname as adminNickname,
				e.player.level as level,
				e.player.lastAccessedAt as lastAccessedAt,
				e.type as type,
				e.eventType as eventType,
				count(e.id) as eventCount,
				max(e.occurredAt) as lastOccurredAt
			from AdClientEvent e
			where e.type in :types
			group by e.player.userKey, e.player.adminNickname, e.player.level, e.player.lastAccessedAt, e.type, e.eventType
			order by count(e.id) desc, max(e.occurredAt) desc
			""")
	List<PlayerAdClientEventTypeCount> findPlayerEventCountsByType(@Param("types") Collection<AdEventType> types);

	@Modifying
	@Query(value = """
			delete from ad_client_events
			where player_id in (
				select id from players where user_key = :userKey
			)
			""", nativeQuery = true)
	int deleteByPlayerUserKey(@Param("userKey") String userKey);

	interface AdClientEventTypeCount {
		AdEventType getType();

		AdClientEventType getEventType();

		long getEventCount();
	}

	interface PlayerAdClientEventTypeCount {
		String getUserKey();

		String getAdminNickname();

		int getLevel();

		Instant getLastAccessedAt();

		AdEventType getType();

		AdClientEventType getEventType();

		long getEventCount();

		Instant getLastOccurredAt();
	}
}
