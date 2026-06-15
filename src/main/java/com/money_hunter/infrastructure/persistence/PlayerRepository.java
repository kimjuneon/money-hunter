package com.money_hunter.infrastructure.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.money_hunter.domain.AdEventType;
import com.money_hunter.domain.Player;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerRepository extends JpaRepository<Player, Long> {
	Optional<Player> findByUserKey(String userKey);

	long countByJobIsNotNull();

	long countBySuspendedAtIsNotNull();

	long countByCreatedAtAfter(Instant createdAt);

	long countByCreatedAtGreaterThanEqual(Instant createdAt);

	long countByCreatedAtBefore(Instant createdAt);

	long countByCreatedAtBetween(Instant startedAt, Instant endedAt);

	long countByCreatedAtGreaterThanEqualAndCreatedAtLessThan(Instant startedAt, Instant endedAt);

	long countByAutoHuntEndsAtAfter(Instant now);

	long countByLastAccessedAtGreaterThanEqual(Instant accessedAt);

	long countByLastAccessedAtGreaterThanEqualAndLastAccessedAtLessThan(Instant startedAt, Instant endedAt);

	long countByJobIsNotNullAndLastAccessedAtGreaterThanEqualAndLastAccessedAtLessThan(Instant startedAt, Instant endedAt);

	@Query("""
			select count(distinct p.id)
			from Player p
			where p.job is not null
				and p.suspendedAt is null
				and p.level >= :minimumLevel
				and exists (
					select s.id
					from LoginSession s
					where s.userKey = p.userKey
						and s.createdAt >= :firstAccessDayStartedAt
						and s.createdAt < :firstAccessDayEndedAt
				)
				and exists (
					select s.id
					from LoginSession s
					where s.userKey = p.userKey
						and s.createdAt >= :secondAccessDayStartedAt
						and s.createdAt < :secondAccessDayEndedAt
				)
				and exists (
					select s.id
					from LoginSession s
					where s.userKey = p.userKey
						and s.createdAt >= :thirdAccessDayStartedAt
						and s.createdAt < :thirdAccessDayEndedAt
				)
				and exists (
					select r.id
					from RewardClaim r
					where r.player = p
				)
				and (
					select count(a.id)
					from AdEvent a
					where a.player = p
						and a.type in :rewardAdTypes
				) >= :minimumRewardAdEvents
				and p.weeklyPunchKingBestScore > 0
				and exists (
					select d.id
					from AdEvent d
					where d.player = p
						and d.type = :dungeonRunType
				)
			""")
	long countLoyalActiveUsersByReferenceDay(
			@Param("firstAccessDayStartedAt") Instant firstAccessDayStartedAt,
			@Param("firstAccessDayEndedAt") Instant firstAccessDayEndedAt,
			@Param("secondAccessDayStartedAt") Instant secondAccessDayStartedAt,
			@Param("secondAccessDayEndedAt") Instant secondAccessDayEndedAt,
			@Param("thirdAccessDayStartedAt") Instant thirdAccessDayStartedAt,
			@Param("thirdAccessDayEndedAt") Instant thirdAccessDayEndedAt,
			@Param("minimumLevel") int minimumLevel,
			@Param("minimumRewardAdEvents") long minimumRewardAdEvents,
			@Param("rewardAdTypes") Collection<AdEventType> rewardAdTypes,
			@Param("dungeonRunType") AdEventType dungeonRunType);

	@Query("""
			select count(distinct p.id)
			from Player p
			where p.lastAccessedAt >= :visitedAtStartedAt
				and p.lastAccessedAt < :visitedAtEndedAt
				and (
					p.job is null
					or p.suspendedAt is not null
					or p.level < :minimumLevel
					or not exists (
						select s.id
						from LoginSession s
						where s.userKey = p.userKey
							and s.createdAt >= :firstAccessDayStartedAt
							and s.createdAt < :firstAccessDayEndedAt
					)
					or not exists (
						select s.id
						from LoginSession s
						where s.userKey = p.userKey
							and s.createdAt >= :secondAccessDayStartedAt
							and s.createdAt < :secondAccessDayEndedAt
					)
					or not exists (
						select s.id
						from LoginSession s
						where s.userKey = p.userKey
							and s.createdAt >= :thirdAccessDayStartedAt
							and s.createdAt < :thirdAccessDayEndedAt
					)
					or not exists (
						select r.id
						from RewardClaim r
						where r.player = p
					)
					or (
						select count(a.id)
						from AdEvent a
						where a.player = p
							and a.type in :rewardAdTypes
					) < :minimumRewardAdEvents
					or p.weeklyPunchKingBestScore <= 0
					or not exists (
						select d.id
						from AdEvent d
						where d.player = p
							and d.type = :dungeonRunType
					)
				)
			""")
	long countNonLoyalVisitorsByReferenceDay(
			@Param("visitedAtStartedAt") Instant visitedAtStartedAt,
			@Param("visitedAtEndedAt") Instant visitedAtEndedAt,
			@Param("firstAccessDayStartedAt") Instant firstAccessDayStartedAt,
			@Param("firstAccessDayEndedAt") Instant firstAccessDayEndedAt,
			@Param("secondAccessDayStartedAt") Instant secondAccessDayStartedAt,
			@Param("secondAccessDayEndedAt") Instant secondAccessDayEndedAt,
			@Param("thirdAccessDayStartedAt") Instant thirdAccessDayStartedAt,
			@Param("thirdAccessDayEndedAt") Instant thirdAccessDayEndedAt,
			@Param("minimumLevel") int minimumLevel,
			@Param("minimumRewardAdEvents") long minimumRewardAdEvents,
			@Param("rewardAdTypes") Collection<AdEventType> rewardAdTypes,
			@Param("dungeonRunType") AdEventType dungeonRunType);

	long countByRookieEventStartedAtIsNotNull();

	long countByRookieEventCompletedDaysGreaterThanEqual(int completedDays);

	long countByRookieEventRewardClaimedAtIsNotNull();

	long countByJobIsNotNullAndRookieEventStartedAtIsNull();

	@Query("select coalesce(sum(p.gold), 0) from Player p")
	long totalGold();

	@Query("""
				select p
				from Player p
				where (
						:favoriteMode = 'ALL'
						or (:favoriteMode = 'FAVORITE' and p.adminFavorite = true)
						or (:favoriteMode = 'NOT_FAVORITE' and p.adminFavorite = false)
					)
					and (
						:statusMode = 'ALL'
						or (:statusMode = 'ACTIVE' and p.suspendedAt is null)
						or (:statusMode = 'SUSPENDED' and p.suspendedAt is not null)
					)
					and (
						:progressMode = 'ALL'
						or (:progressMode = 'ONBOARDED' and p.job is not null)
						or (:progressMode = 'NEEDS_JOB' and p.job is null)
						or (:progressMode = 'FEATURE_TUTORIAL_DONE' and p.featureTutorialCompletedAt is not null)
					)
					and (:hiddenSkinsOnly = false or coalesce(p.ownedPetSkinKeys, '') like '%EASTER_EGG%')
					and (:activeAutoHuntOnly = false or (p.autoHuntEndsAt is not null and p.autoHuntEndsAt > :now))
					and (
						:query is null
					or :query = ''
					or lower(p.userKey) like lower(concat('%', :query, '%'))
					or lower(coalesce(p.adminNickname, '')) like lower(concat('%', :query, '%'))
					or lower(coalesce(p.gameProfileNickname, '')) like lower(concat('%', :query, '%'))
				)
				""")
			org.springframework.data.domain.Page<Player> searchPlayers(
					@Param("query") String query,
				@Param("favoriteMode") String favoriteMode,
				@Param("statusMode") String statusMode,
				@Param("progressMode") String progressMode,
				@Param("hiddenSkinsOnly") boolean hiddenSkinsOnly,
				@Param("activeAutoHuntOnly") boolean activeAutoHuntOnly,
					@Param("now") Instant now,
					org.springframework.data.domain.Pageable pageable);

		@Query("""
					select p
					from Player p
					where (
							:favoriteMode = 'ALL'
							or (:favoriteMode = 'FAVORITE' and p.adminFavorite = true)
							or (:favoriteMode = 'NOT_FAVORITE' and p.adminFavorite = false)
						)
						and (
							:statusMode = 'ALL'
							or (:statusMode = 'ACTIVE' and p.suspendedAt is null)
							or (:statusMode = 'SUSPENDED' and p.suspendedAt is not null)
						)
						and (
							:progressMode = 'ALL'
							or (:progressMode = 'ONBOARDED' and p.job is not null)
							or (:progressMode = 'NEEDS_JOB' and p.job is null)
							or (:progressMode = 'FEATURE_TUTORIAL_DONE' and p.featureTutorialCompletedAt is not null)
						)
						and (:hiddenSkinsOnly = false or coalesce(p.ownedPetSkinKeys, '') like '%EASTER_EGG%')
						and (:activeAutoHuntOnly = false or (p.autoHuntEndsAt is not null and p.autoHuntEndsAt > :now))
						and (
							:query is null
						or :query = ''
						or lower(p.userKey) like lower(concat('%', :query, '%'))
						or lower(coalesce(p.adminNickname, '')) like lower(concat('%', :query, '%'))
						or lower(coalesce(p.gameProfileNickname, '')) like lower(concat('%', :query, '%'))
					)
					""")
		List<Player> searchPlayers(
				@Param("query") String query,
				@Param("favoriteMode") String favoriteMode,
				@Param("statusMode") String statusMode,
				@Param("progressMode") String progressMode,
				@Param("hiddenSkinsOnly") boolean hiddenSkinsOnly,
				@Param("activeAutoHuntOnly") boolean activeAutoHuntOnly,
				@Param("now") Instant now,
				Sort sort);

	@Query("""
			select p.userKey as userKey,
				p.gold as gold,
				p.level as level,
				p.skillPoints as skillPoints,
				p.updatedAt as updatedAt
			from Player p
			where p.gold >= :minimumGold
			order by p.gold desc, p.updatedAt desc
			""")
	List<PlayerGoldSnapshot> findPlayersWithGoldAtLeast(@Param("minimumGold") long minimumGold, org.springframework.data.domain.Pageable pageable);

	@Query("""
			select p.userKey as userKey,
				p.gold as gold,
				p.level as level,
				p.skillPoints as skillPoints,
				p.updatedAt as updatedAt
			from Player p
			where p.skillPoints >= :minimumSkillPoints
			order by p.skillPoints desc, p.updatedAt desc
			""")
	List<PlayerGoldSnapshot> findPlayersWithSkillPointsAtLeast(
			@Param("minimumSkillPoints") int minimumSkillPoints,
			org.springframework.data.domain.Pageable pageable);

	@Query("""
			select p.userKey as userKey,
				p.autoHuntEndsAt as autoHuntEndsAt,
				p.updatedAt as updatedAt
			from Player p
			where p.autoHuntEndsAt is not null and p.autoHuntEndsAt > :maxAutoHuntEnd
			order by p.updatedAt desc
			""")
	List<PlayerTimerSnapshot> findPlayersWithTimersBeyond(
			@Param("maxAutoHuntEnd") Instant maxAutoHuntEnd,
			org.springframework.data.domain.Pageable pageable);

	@Query("""
			select p
			from Player p
			where p.autoHuntEndsAt is not null
				and p.autoHuntEndsAt <= :now
				and p.autoHuntEndNotifiedAt is null
				and (
					p.autoHuntEndSmartMessageAttemptedAt is null
					or p.autoHuntEndSmartMessageAttemptedAt <= :retryBefore
				)
				and p.job is not null
				and p.suspendedAt is null
			""")
	List<Player> findAutoHuntEndedNotificationTargets(
			@Param("now") Instant now,
			@Param("retryBefore") Instant retryBefore);

	@Query("""
			select p
			from Player p
			where p.autoHuntEndsAt is not null
				and p.autoHuntEndsAt > :notifyAfter
				and p.autoHuntEndsAt <= :notifyBefore
				and p.job is not null
				and p.suspendedAt is null
				and (
					p.autoHuntEndingSoonNotificationEndsAt is null
					or p.autoHuntEndingSoonNotificationEndsAt <> p.autoHuntEndsAt
				)
				and (
					p.lastAutoHuntAdClaimedAt is null
					or p.lastAutoHuntAdClaimedAt <= :cooldownReadyBefore
				)
			order by p.autoHuntEndsAt asc, p.id asc
			""")
	List<Player> findAutoHuntEndingSoonNotificationTargets(
			@Param("notifyAfter") Instant notifyAfter,
			@Param("notifyBefore") Instant notifyBefore,
			@Param("cooldownReadyBefore") Instant cooldownReadyBefore,
			org.springframework.data.domain.Pageable pageable);

	@Query("""
			select p
			from Player p
			where p.rookieEventStartedAt is not null
				and p.rookieEventStartedAt >= :startedAfter
				and p.rookieEventMissionNotificationAgreedAt is not null
				and p.rookieEventRewardClaimedAt is null
				and p.rookieEventCompletedDays < :maxEventDays
				and p.job is not null
				and p.suspendedAt is null
				and (
					p.rookieEventMissionMessageSentDate is null
					or p.rookieEventMissionMessageSentDate < :today
				)
			order by p.rookieEventStartedAt asc, p.id asc
			""")
	List<Player> findRookieEventMissionNotificationTargets(
			@Param("today") LocalDate today,
			@Param("startedAfter") Instant startedAfter,
			@Param("maxEventDays") int maxEventDays,
			org.springframework.data.domain.Pageable pageable);

	@Query("""
			select p
			from Player p
			where p.lastAccessedAt <= :inactiveSince
				and p.job is not null
				and p.suspendedAt is null
				and (
					p.dormantSpRewardSentStage < :maxStage
					or p.dormantSpRewardStreakAccessedAt is null
					or p.dormantSpRewardStreakAccessedAt <> p.lastAccessedAt
				)
				and (
					p.dormantSpRewardLastSentAt is null
					or p.dormantSpRewardLastSentAt <= :repeatBefore
					or p.dormantSpRewardStreakAccessedAt is null
					or p.dormantSpRewardStreakAccessedAt <> p.lastAccessedAt
				)
			order by p.lastAccessedAt asc, p.id asc
			""")
	List<Player> findDormantSpRewardNotificationTargets(
			@Param("inactiveSince") Instant inactiveSince,
			@Param("repeatBefore") Instant repeatBefore,
			@Param("maxStage") int maxStage,
			org.springframework.data.domain.Pageable pageable);

	@Query("""
			select p
			from Player p
			where p.job is not null
				and p.suspendedAt is null
				and p.dungeonCouponHuntMillis >= :requiredHuntMillis
				and p.dungeonRunCountDate = :today
				and p.dungeonRunCount < :dailyLimit
				and (p.dungeonNextAvailableAt is null or p.dungeonNextAvailableAt <= :now)
				and (
					p.dungeonExploreAvailableNotificationDate is null
					or p.dungeonExploreAvailableNotificationDate <> :today
					or p.dungeonExploreAvailableNotificationRunCount is null
					or p.dungeonExploreAvailableNotificationRunCount <> p.dungeonRunCount
				)
			order by p.id asc
			""")
	List<Player> findDungeonExploreAvailableNotificationTargets(
			@Param("now") Instant now,
			@Param("today") LocalDate today,
			@Param("dailyLimit") int dailyLimit,
			@Param("requiredHuntMillis") long requiredHuntMillis,
			org.springframework.data.domain.Pageable pageable);

	@Modifying
	@Query("""
			update Player p
			set p.dungeonExploreAvailableNotificationDate = :today,
				p.dungeonExploreAvailableNotificationRunCount = :runCount,
				p.dungeonExploreAvailableNotificationSentAt = :sentAt,
				p.updatedAt = :sentAt
			where p.id = :playerId
				and p.job is not null
				and p.suspendedAt is null
				and p.dungeonCouponHuntMillis >= :requiredHuntMillis
				and p.dungeonRunCountDate = :today
				and p.dungeonRunCount = :runCount
				and p.dungeonRunCount < :dailyLimit
				and (p.dungeonNextAvailableAt is null or p.dungeonNextAvailableAt <= :sentAt)
				and (
					p.dungeonExploreAvailableNotificationDate is null
					or p.dungeonExploreAvailableNotificationDate <> :today
					or p.dungeonExploreAvailableNotificationRunCount is null
					or p.dungeonExploreAvailableNotificationRunCount <> :runCount
				)
			""")
	int claimDungeonExploreAvailableNotification(
			@Param("playerId") Long playerId,
			@Param("today") LocalDate today,
			@Param("runCount") int runCount,
			@Param("dailyLimit") int dailyLimit,
			@Param("requiredHuntMillis") long requiredHuntMillis,
			@Param("sentAt") Instant sentAt);

	@Query("""
			select p
			from Player p
			where p.lastAccessedAt <= :inactiveSince
				and p.lastAccessedAt > :expiredBefore
				and p.job is not null
				and p.suspendedAt is null
				and (
					p.battleReadyDailySentStage < :maxStage
					or p.battleReadyDailyStreakAccessedAt is null
					or p.battleReadyDailyStreakAccessedAt <> p.lastAccessedAt
				)
			order by p.lastAccessedAt asc, p.id asc
			""")
	List<Player> findBattleReadyDailyNotificationTargets(
			@Param("inactiveSince") Instant inactiveSince,
			@Param("expiredBefore") Instant expiredBefore,
			@Param("maxStage") int maxStage,
			org.springframework.data.domain.Pageable pageable);

	@Modifying
	@Query(value = """
				insert into players (
					user_key, character_slots, gold, skill_points, level, experience,
					current_monster_key, current_monster_hp, defeated_monsters,
					last_settled_at, created_at, last_accessed_at, updated_at
				)
				values (:userKey, 1, 0, 0, 1, 0, 'BOSS_ROCK', 120, 0, :now, :now, :now, :now)
				on conflict (user_key) do nothing
				""", nativeQuery = true)
	void insertIfAbsent(@Param("userKey") String userKey, @Param("now") Instant now);

	interface PlayerGoldSnapshot {
		String getUserKey();

		long getGold();

		int getLevel();

		int getSkillPoints();

		Instant getUpdatedAt();
	}

	interface PlayerTimerSnapshot {
		String getUserKey();

		Instant getAutoHuntEndsAt();

		Instant getUpdatedAt();
	}
}
