package com.money_hunter.infrastructure.persistence;

import java.util.Optional;
import java.time.Instant;
import java.util.List;

import com.money_hunter.domain.Player;
import com.money_hunter.domain.RewardClaim;
import com.money_hunter.domain.RewardClaimStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RewardClaimRepository extends JpaRepository<RewardClaim, Long> {
	Optional<RewardClaim> findByPlayerAndIdempotencyKey(Player player, String idempotencyKey);

	long deleteByPlayerUserKey(String userKey);

	long countByCreatedAtAfter(Instant createdAt);

	long countByStatus(RewardClaimStatus status);

	@Query("select coalesce(sum(r.pointAmount), 0) from RewardClaim r where r.createdAt >= :from")
	long sumPointAmountSince(@Param("from") Instant from);

	@Query("select coalesce(sum(r.pointAmount), 0) from RewardClaim r where r.status = :status")
	long sumPointAmountByStatus(@Param("status") RewardClaimStatus status);

	@Query("""
			select r.player.userKey as userKey,
				count(r.id) as claimCount,
				coalesce(sum(r.pointAmount), 0) as pointAmount,
				max(r.createdAt) as lastClaimedAt
			from RewardClaim r
			where r.createdAt >= :from
			group by r.player.userKey
			having count(r.id) >= :minimumCount
			order by count(r.id) desc, max(r.createdAt) desc
			""")
	List<PlayerRewardClaimCount> findPlayerClaimCountsSince(
			@Param("from") Instant from,
			@Param("minimumCount") long minimumCount,
			Pageable pageable);

	interface PlayerRewardClaimCount {
		String getUserKey();

		long getClaimCount();

		long getPointAmount();

		Instant getLastClaimedAt();
	}
}
