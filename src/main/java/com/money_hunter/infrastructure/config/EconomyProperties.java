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
		long boostAdSeconds,
		long maxAdSeconds,
		long skillPointAdCooldownSeconds,
		long rewardGoldThreshold,
		int rewardPointAmount
) {
}
