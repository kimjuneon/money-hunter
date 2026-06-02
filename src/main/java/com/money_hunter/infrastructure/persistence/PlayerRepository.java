package com.money_hunter.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.money_hunter.domain.Player;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerRepository extends JpaRepository<Player, Long> {
	Optional<Player> findByUserKey(String userKey);

	long countByJobIsNotNull();

	long countBySuspendedAtIsNotNull();

	long countByCreatedAtAfter(Instant createdAt);

	long countByCreatedAtBefore(Instant createdAt);

	long countByCreatedAtBetween(Instant startedAt, Instant endedAt);

	long countByAutoHuntEndsAtAfter(Instant now);

	long countByBoostEndsAtAfter(Instant now);

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
				p.boostEndsAt as boostEndsAt,
				p.updatedAt as updatedAt
			from Player p
			where (p.autoHuntEndsAt is not null and p.autoHuntEndsAt > :maxAutoHuntEnd)
				or (p.boostEndsAt is not null and p.boostEndsAt > :maxBoostEnd)
			order by p.updatedAt desc
			""")
	List<PlayerTimerSnapshot> findPlayersWithTimersBeyond(
			@Param("maxAutoHuntEnd") Instant maxAutoHuntEnd,
			@Param("maxBoostEnd") Instant maxBoostEnd,
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

		Instant getBoostEndsAt();

		Instant getUpdatedAt();
	}
}
