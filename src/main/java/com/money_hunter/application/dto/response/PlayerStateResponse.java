package com.money_hunter.application.dto.response;

import java.time.Instant;
import java.util.List;

import com.money_hunter.domain.JobType;

public record PlayerStateResponse(
		String userKey,
		JobType job,
		boolean onboardingRequired,
		boolean tutorialRewardClaimed,
		int characterSlots,
		int maxCharacterSlots,
		int companionPriceWon,
		int skillPointPackPriceWon,
		int skillPointPackAmount,
		long gold,
		long rewardGoldThreshold,
		int rewardPointAmount,
		int adRevenuePerRewardAdWon,
		int goldPerTossPoint,
		int payoutRatePercent,
		int rewardProgressPercent,
		boolean rewardClaimable,
		int skillPoints,
		boolean skillPointRewardsAvailable,
		int friendInviteRewardCount,
		int friendInviteLimit,
		int friendInviteRewardSkillPoints,
		int level,
			long experience,
			int nextLevelExperience,
			long goldPerHour,
			int attackIntervalMillis,
			Instant autoHuntEndsAt,
			Instant boostEndsAt,
		MonsterResponse monster,
		List<SkillResponse> skills,
		NotificationResponse latestNotification
) {
}
