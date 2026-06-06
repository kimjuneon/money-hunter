package com.money_hunter.application.dto.response;

public record BossRaidRewardResponse(
		String bossName,
		String difficultyName,
		String rewardType,
		String rewardLabel,
		long amount,
		PlayerStateResponse state
) {
}
