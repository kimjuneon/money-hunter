package com.money_hunter.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "money-hunter.economy")
public record EconomyProperties(
		int adRevenuePerRewardAdWon,
		int goldPerTossPoint,
		int companionPriceWon,
		int skillPointPackPriceWon,
		int skillPointPackAmount,
		int friendInviteRewardSkillPoints,
			int friendInviteLimit,
			int maxCharacterSlots,
			long autoHuntAdSeconds,
			long autoHuntAdCooldownSeconds,
			long maxAdSeconds,
		int dungeonFreeDailyLimit,
		int dungeonAdditionalDailyLimit,
		long dungeonReentryCooldownSeconds,
		long skillPointAdCooldownSeconds,
		long weeklyPunchKingMaxGoldReward,
		long weeklyPunchKingGoldRewardScoreScale,
		int weeklyPunchKingBaseSkillPoints,
		long weeklyPunchKingSkillPointTier2Score,
		int weeklyPunchKingSkillPointTier2Reward,
		long weeklyPunchKingSkillPointTier3Score,
		int weeklyPunchKingSkillPointTier3Reward,
		long weeklyPunchKingSkillPointTier4Score,
		int weeklyPunchKingSkillPointTier4Reward,
		long rewardGoldThreshold,
		int rewardPointAmount,
		int anomalyLimitPerRule,
		long anomalyAdEventsPerHourWarning,
		long anomalyRewardClaimsPerDayWarning,
		long anomalyGoldThresholdMultiplier,
		int anomalySkillPointsWarning,
		long anomalyTimerGraceSeconds
) {
}
