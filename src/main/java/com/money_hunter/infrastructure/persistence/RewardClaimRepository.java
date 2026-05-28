package com.money_hunter.infrastructure.persistence;

import java.util.Optional;
import java.time.Instant;

import com.money_hunter.domain.Player;
import com.money_hunter.domain.RewardClaim;
import com.money_hunter.domain.RewardClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RewardClaimRepository extends JpaRepository<RewardClaim, Long> {
	Optional<RewardClaim> findByPlayerAndIdempotencyKey(Player player, String idempotencyKey);

	long countByCreatedAtAfter(Instant createdAt);

	long countByStatus(RewardClaimStatus status);

	@Query("select coalesce(sum(r.pointAmount), 0) from RewardClaim r where r.createdAt >= :from")
	long sumPointAmountSince(@Param("from") Instant from);

	@Query("select coalesce(sum(r.pointAmount), 0) from RewardClaim r where r.status = :status")
	long sumPointAmountByStatus(@Param("status") RewardClaimStatus status);
}
