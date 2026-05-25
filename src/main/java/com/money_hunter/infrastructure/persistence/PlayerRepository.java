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

	@Query("""
			select p
			from Player p
			where p.autoHuntEndsAt is not null
				and p.autoHuntEndsAt <= :now
				and p.autoHuntEndNotifiedAt is null
				and p.job is not null
			""")
	List<Player> findAutoHuntEndedNotificationTargets(@Param("now") Instant now);

	@Modifying
	@Query(value = """
			insert into players (
				user_key, character_slots, gold, skill_points, level, experience,
				current_monster_key, current_monster_hp, defeated_monsters,
				last_settled_at, created_at, updated_at
			)
			values (:userKey, 1, 0, 0, 1, 0, 'BOSS_ROCK', 120, 0, :now, :now, :now)
			on conflict (user_key) do nothing
			""", nativeQuery = true)
	void insertIfAbsent(@Param("userKey") String userKey, @Param("now") Instant now);
}
