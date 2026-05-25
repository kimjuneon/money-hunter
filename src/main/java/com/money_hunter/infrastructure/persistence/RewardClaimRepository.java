package com.money_hunter.infrastructure.persistence;

import java.util.Optional;

import com.money_hunter.domain.Player;
import com.money_hunter.domain.RewardClaim;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardClaimRepository extends JpaRepository<RewardClaim, Long> {
	Optional<RewardClaim> findByPlayerAndIdempotencyKey(Player player, String idempotencyKey);
}
