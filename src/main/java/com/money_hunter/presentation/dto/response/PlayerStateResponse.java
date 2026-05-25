package com.money_hunter.presentation.dto.response;

import java.time.Instant;
import java.util.List;

import com.money_hunter.domain.JobType;

public record PlayerStateResponse(
		String userKey,
		JobType job,
		boolean onboardingRequired,
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
		int friendInviteRewardCount,
		int friendInviteLimit,
		int friendInviteRewardSkillPoints,
		int level,
		long experience,
		int nextLevelExperience,
		long goldPerHour,
		Instant autoHuntEndsAt,
		Instant boostEndsAt,
		MonsterResponse monster,
		List<SkillResponse> skills,
		NotificationResponse latestNotification
) {
}
