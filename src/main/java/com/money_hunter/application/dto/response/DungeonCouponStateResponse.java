package com.money_hunter.application.dto.response;

import java.time.Instant;
import java.util.List;

public record DungeonCouponStateResponse(
		boolean enabled,
		int count,
		int bossTicketCount,
		int dungeonRunsToday,
		int dungeonDailyLimit,
		int dungeonFreeDailyLimit,
		int dungeonAdditionalDailyLimit,
		int dungeonRemainingRuns,
		long dungeonHuntProgressSeconds,
		long dungeonHuntRequiredSeconds,
		boolean dungeonHuntRequirementCompleted,
		Instant dungeonNextAvailableAt,
		long dungeonCooldownSeconds,
		boolean dungeonAvailable,
		String dungeonUnavailableReason,
		String tierName,
		String rewardPreview,
		String bossName,
		String bossDifficultyName,
		String bossRewardPreview,
		List<AdventureRewardPoolItemResponse> dungeonRewards,
		List<AdventureRewardPoolItemResponse> bossRewards
) {
}
