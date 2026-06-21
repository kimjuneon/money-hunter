package com.money_hunter.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;

import com.money_hunter.domain.PlayerDailyAccess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerDailyAccessRepository extends JpaRepository<PlayerDailyAccess, Long> {
	@Modifying
	@Query(value = """
			insert into player_daily_accesses (player_id, access_date, first_accessed_at, last_accessed_at)
			values (:playerId, :accessDate, :now, :now)
			on conflict (player_id, access_date)
			do update set last_accessed_at = excluded.last_accessed_at
			""", nativeQuery = true)
	int upsertAccess(
			@Param("playerId") Long playerId,
			@Param("accessDate") LocalDate accessDate,
			@Param("now") Instant now);

	@Modifying
	@Query("""
			delete from PlayerDailyAccess dailyAccess
			where dailyAccess.accessDate < :accessDate
			""")
	int deleteBefore(@Param("accessDate") LocalDate accessDate);

	@Query("""
			select count(distinct dailyAccess.accessDate)
			from PlayerDailyAccess dailyAccess
			where dailyAccess.accessDate >= :startedAt
				and dailyAccess.accessDate <= :endedAt
			""")
	long countAccessDatesBetween(
			@Param("startedAt") LocalDate startedAt,
			@Param("endedAt") LocalDate endedAt);

	@Query("""
			select count(dailyAccess.id)
			from PlayerDailyAccess dailyAccess
			where dailyAccess.accessDate >= :startedAt
				and dailyAccess.accessDate <= :endedAt
			""")
	long countActiveUserDaysBetween(
			@Param("startedAt") LocalDate startedAt,
			@Param("endedAt") LocalDate endedAt);
}
