package com.money_hunter.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import com.money_hunter.domain.RewardClaimStatus;
import com.money_hunter.infrastructure.config.AppProperties;
import com.money_hunter.infrastructure.persistence.AdEventRepository;
import com.money_hunter.infrastructure.persistence.PlayerRepository;
import com.money_hunter.infrastructure.persistence.RewardClaimRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminMonitoringService {
	private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

	private final PlayerRepository playerRepository;
	private final AdEventRepository adEventRepository;
	private final RewardClaimRepository rewardClaimRepository;
	private final RuntimeEconomyService economy;
	private final AppProperties appProperties;
	private final Clock clock = Clock.systemUTC();

	public AdminMonitoringService(
			PlayerRepository playerRepository,
			AdEventRepository adEventRepository,
			RewardClaimRepository rewardClaimRepository,
			RuntimeEconomyService economy,
			AppProperties appProperties
	) {
		this.playerRepository = playerRepository;
		this.adEventRepository = adEventRepository;
		this.rewardClaimRepository = rewardClaimRepository;
		this.economy = economy;
		this.appProperties = appProperties;
	}

	public AdminOverview overview() {
		Instant now = Instant.now(clock);
		Instant today = LocalDate.now(SEOUL).atStartOfDay(SEOUL).toInstant();
		long adEventsToday = adEventRepository.countByOccurredAtAfter(today);
		long rewardAdEventsToday = adEventsToday;
		long rewardClaimsToday = rewardClaimRepository.countByCreatedAtAfter(today);
		long rewardPointsToday = rewardClaimRepository.sumPointAmountSince(today);
		long pendingRewardClaims = rewardClaimRepository.countByStatus(RewardClaimStatus.PENDING_PROMOTION_GRANT);
		long pendingRewardPoints = rewardClaimRepository.sumPointAmountByStatus(RewardClaimStatus.PENDING_PROMOTION_GRANT);
		long estimatedAdRevenueWonToday = rewardAdEventsToday * economy.adRevenuePerRewardAdWon();
		long estimatedRewardCostWonToday = rewardPointsToday;

		return new AdminOverview(
				now,
				appProperties.integrationMode(),
				appProperties.distributionTarget(),
				appProperties.tossReleaseReady(),
				appProperties.releaseBlockers(),
				playerRepository.count(),
				playerRepository.countByJobIsNotNull(),
				playerRepository.countByCreatedAtAfter(today),
				playerRepository.countByAutoHuntEndsAtAfter(now),
				playerRepository.countByBoostEndsAtAfter(now),
				playerRepository.totalGold(),
				rewardAdEventsToday,
				rewardClaimsToday,
				rewardPointsToday,
				pendingRewardClaims,
				pendingRewardPoints,
				estimatedAdRevenueWonToday,
				estimatedRewardCostWonToday,
				estimatedAdRevenueWonToday - estimatedRewardCostWonToday,
				economy.snapshot()
		);
	}

	public record AdminOverview(
			Instant generatedAt,
			String integrationMode,
			String distributionTarget,
			boolean tossReleaseReady,
			java.util.List<String> releaseBlockers,
			long totalPlayers,
			long onboardedPlayers,
			long newPlayersToday,
			long activeAutoHuntPlayers,
			long activeBoostPlayers,
			long totalGoldInCirculation,
			long rewardAdEventsToday,
			long rewardClaimsToday,
			long rewardPointsToday,
			long pendingRewardClaims,
			long pendingRewardPoints,
			long estimatedAdRevenueWonToday,
			long estimatedRewardCostWonToday,
			long estimatedNetWonToday,
			EconomyPolicySnapshot economy
	) {
	}
}
