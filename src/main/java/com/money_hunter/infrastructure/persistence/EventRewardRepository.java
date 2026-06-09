package com.money_hunter.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.money_hunter.domain.EventReward;
import com.money_hunter.domain.Player;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRewardRepository extends JpaRepository<EventReward, Long> {
	List<EventReward> findByPlayerAndExpiresAtAfterOrderByClaimedAtAscCreatedAtDesc(Player player, Instant now);

	Optional<EventReward> findByIdAndPlayerUserKey(Long id, String userKey);

	Optional<EventReward> findByPlayerAndRewardKey(Player player, String rewardKey);

	boolean existsByPlayerAndRewardKey(Player player, String rewardKey);

	long deleteByPlayerAndExpiresAtLessThanEqual(Player player, Instant now);

	long deleteByPlayer(Player player);
}
